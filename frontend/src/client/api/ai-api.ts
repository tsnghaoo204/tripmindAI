import { request } from "../client"
import { ApiError, DEFAULT_ERROR_MESSAGE } from "../errors"
import { isAbortError, openSseStream, parseJson } from "../sse"
import type {
  ApplyProposalResponse,
  ChatMessage,
  Conversation,
  ExplanationResponse,
  GenerateJobStatus,
  Proposal,
  UndoProposalResponse,
} from "../types"

// Chat SSE payloads, see AgentService / ToolExecutor / ProposalService
export interface ChatTokenEvent {
  text: string
}
export interface ChatToolStartEvent {
  tool: string
  label?: string
}
export interface ChatToolEndEvent {
  tool: string
  ms?: number
  status?: string
}
export interface ChatProposalEvent {
  proposalId: number
  summary: string
  estimatedCostDelta: number
  travelTimeDelta: number
  /** Number of changes in the proposal. */
  changes: number
}
export interface ChatDoneEvent {
  messageId: number
  conversationId: number
}
export interface StreamErrorEvent {
  code?: string
  message?: string
}

export interface ChatCallbacks {
  onToken?: (text: string) => void
  onToolStart?: (evt: ChatToolStartEvent) => void
  onToolEnd?: (evt: ChatToolEndEvent) => void
  onProposal?: (evt: ChatProposalEvent) => void
  /** Called exactly once when the turn finishes; payload is null if the stream ended without a done event. */
  onDone?: (evt: ChatDoneEvent | null) => void
  onError?: (err: ApiError) => void
}

export interface GenerateCallbacks {
  onProgress?: (status: GenerateJobStatus) => void
  onDone?: (status: GenerateJobStatus) => void
  onError?: (err: ApiError) => void
  /** The stream closed before a done or error event. */
  onInterrupted?: () => void
}

function toApiError(err: unknown): ApiError {
  if (err instanceof ApiError) return err
  return new ApiError(err instanceof Error && err.message ? err.message : DEFAULT_ERROR_MESSAGE)
}

export const aiApi = {
  getConversations: (tripId: number) => request<Conversation[]>(`/api/trips/${tripId}/ai/conversations`),

  getMessages: (conversationId: number) => request<ChatMessage[]>(`/api/conversations/${conversationId}/messages`),

  getProposal: (proposalId: number) => request<Proposal>(`/api/proposals/${proposalId}`),

  applyProposal: (tripId: number, proposalId: number) =>
    request<ApplyProposalResponse>(`/api/trips/${tripId}/ai/apply`, { method: "POST", data: { proposalId } }),

  rejectProposal: (proposalId: number) => request<Proposal>(`/api/proposals/${proposalId}/reject`, { method: "POST" }),

  previewUndo: (tripId: number, proposalId: number) =>
    request<UndoProposalResponse>(`/api/trips/${tripId}/ai/undo/${proposalId}/preview`),

  undoProposal: (tripId: number, proposalId: number) =>
    request<UndoProposalResponse>(`/api/trips/${tripId}/ai/undo`, { method: "POST", data: { proposalId } }),

  getExplanation: (activityId: number) => request<ExplanationResponse>(`/api/activities/${activityId}/explanation`),

  startGenerateItinerary: (tripId: number) =>
    request<GenerateJobStatus>(`/api/trips/${tripId}/ai/generate`, { method: "POST" }),

  getGenerateStatus: (tripId: number, jobId: string) =>
    request<GenerateJobStatus>(`/api/trips/${tripId}/ai/generate/${jobId}`),

  /** Streams generation progress; returns an abort function. */
  streamGenerateStatus: (tripId: number, jobId: string, callbacks: GenerateCallbacks): (() => void) => {
    const controller = new AbortController()
    let finished = false

    openSseStream({
      path: `/api/trips/${tripId}/ai/generate/${jobId}/stream`,
      signal: controller.signal,
      onEvent: ({ event, data }) => {
        if (finished) return
        const parsed = parseJson<GenerateJobStatus>(data)
        if (!parsed) return
        if (event === "progress") callbacks.onProgress?.(parsed)
        else if (event === "done") {
          finished = true
          callbacks.onDone?.(parsed)
        } else if (event === "error") {
          finished = true
          callbacks.onError?.(new ApiError(parsed.error || "Không thể sinh lịch trình"))
        }
      },
    })
      .then(() => {
        if (!finished && !controller.signal.aborted) callbacks.onInterrupted?.()
      })
      .catch((err) => {
        if (finished || controller.signal.aborted || isAbortError(err)) return
        finished = true
        callbacks.onError?.(toApiError(err))
      })

    return () => controller.abort()
  },

  /** Streams one assistant turn; returns an abort function. */
  streamChat: (tripId: number, data: { message: string; conversationId?: number }, callbacks: ChatCallbacks) => {
    const controller = new AbortController()
    let finished = false
    const finish = (fn: () => void) => {
      if (finished) return
      finished = true
      fn()
    }

    openSseStream({
      path: `/api/trips/${tripId}/ai/chat`,
      method: "POST",
      body: data,
      signal: controller.signal,
      onEvent: ({ event, data: raw }) => {
        if (finished) return
        if (event === "token") {
          const parsed = parseJson<ChatTokenEvent>(raw)
          callbacks.onToken?.(parsed && typeof parsed.text === "string" ? parsed.text : raw)
          return
        }
        const parsed = parseJson<Record<string, unknown>>(raw)
        if (!parsed) return
        if (event === "tool_start") callbacks.onToolStart?.(parsed as unknown as ChatToolStartEvent)
        else if (event === "tool_end") callbacks.onToolEnd?.(parsed as unknown as ChatToolEndEvent)
        else if (event === "proposal") callbacks.onProposal?.(parsed as unknown as ChatProposalEvent)
        else if (event === "done") finish(() => callbacks.onDone?.(parsed as unknown as ChatDoneEvent))
        else if (event === "error") {
          const e = parsed as StreamErrorEvent
          finish(() => callbacks.onError?.(new ApiError(e.message || "Lỗi giao tiếp với Trợ lý AI", { code: e.code })))
        }
      },
    })
      .then(() => {
        if (!controller.signal.aborted) finish(() => callbacks.onDone?.(null))
      })
      .catch((err) => {
        if (controller.signal.aborted || isAbortError(err)) return
        finish(() => callbacks.onError?.(toApiError(err)))
      })

    return () => {
      finished = true
      controller.abort()
    }
  },
}
