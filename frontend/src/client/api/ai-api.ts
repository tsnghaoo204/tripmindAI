import { apiClient, request } from "../client"

export interface Conversation {
  id: number
  tripId: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  conversationId: number
  role: "USER" | "ASSISTANT" | "SYSTEM"
  content: string
  attachments?: any
  createdAt: string
}

export interface ProposalDetail {
  id: number
  tripId: number
  summary: string
  status: "PENDING" | "APPLIED" | "REJECTED" | "EXPIRED" | "REVERTED"
  changes: Array<{
    op: "ADD" | "REMOVE" | "UPDATE" | "REORDER"
    dayNumber?: number
    title?: string
    reason?: string
    estimatedCost?: number
    place?: { name: string; externalId: string }
  }>
  estimatedCostDelta: number
  travelTimeDelta: number
  expiresAt: string
}

export interface UndoPreview {
  proposalId: number
  canUndo: boolean
  expiresInSeconds?: number
  itemsToRevert: Array<{
    activityId: number
    action: string
    title: string
    isModifiedByUser: boolean
  }>
}

export interface ExplanationResponse {
  activityId: number
  known: boolean
  createdBy: string
  proposalSummary?: string
  proposalReason?: string
  changeReason?: string
  appliedAt?: string
  tools?: Array<{
    toolName: string
    arguments: string
    executionTimeMs: number
  }>
  rejected?: Array<{
    dayNumber: number
    title: string
    reason: string
  }>
}

export interface GenerateJobStatus {
  jobId: string
  state: "QUEUED" | "RUNNING" | "DONE" | "FAILED"
  step: string
  progress: number
  result?: {
    proposalId: number
    activitiesCreated: number
    rejectedCount: number
    undoAvailableUntil: string
  }
  error?: string
}

export const aiApi = {
  getConversations: async (tripId: number): Promise<Conversation[]> => {
    return request<Conversation[]>(`/api/trips/${tripId}/ai/conversations`)
  },

  getMessages: async (conversationId: number): Promise<ChatMessage[]> => {
    return request<ChatMessage[]>(`/api/conversations/${conversationId}/messages`)
  },

  getProposal: async (proposalId: number): Promise<ProposalDetail> => {
    return request<ProposalDetail>(`/api/proposals/${proposalId}`)
  },

  applyProposal: async (tripId: number, proposalId: number): Promise<any> => {
    return request<any>(`/api/trips/${tripId}/ai/apply`, {
      method: "POST",
      data: { proposalId },
    })
  },

  rejectProposal: async (proposalId: number): Promise<void> => {
    return request<void>(`/api/proposals/${proposalId}/reject`, {
      method: "POST",
    })
  },

  previewUndo: async (tripId: number, proposalId: number): Promise<UndoPreview> => {
    return request<UndoPreview>(`/api/trips/${tripId}/ai/undo/${proposalId}/preview`)
  },

  undoProposal: async (tripId: number, proposalId: number): Promise<any> => {
    return request<any>(`/api/trips/${tripId}/ai/undo`, {
      method: "POST",
      data: { proposalId },
    })
  },

  getExplanation: async (activityId: number): Promise<ExplanationResponse> => {
    return request<ExplanationResponse>(`/api/activities/${activityId}/explanation`)
  },

  startGenerateItinerary: async (tripId: number): Promise<GenerateJobStatus> => {
    return request<GenerateJobStatus>(`/api/trips/${tripId}/ai/generate`, {
      method: "POST",
    })
  },

  getGenerateStatus: async (tripId: number, jobId: string): Promise<GenerateJobStatus> => {
    return request<GenerateJobStatus>(`/api/trips/${tripId}/ai/generate/${jobId}`)
  },

  /** Stream SSE chat */
  streamChat: (
    tripId: number,
    data: { message: string; conversationId?: number },
    callbacks: {
      onToken?: (token: string) => void
      onToolStart?: (tool: string) => void
      onToolEnd?: (tool: string) => void
      onProposal?: (proposal: any) => void
      onDone?: () => void
      onError?: (err: any) => void
    },
  ) => {
    const token = localStorage.getItem("access_token")
    const url = `/api/trips/${tripId}/ai/chat`

    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
    })
      .then(async (response) => {
        if (!response.ok) {
          const errJson = await response.json().catch(() => ({}))
          throw new Error(errJson.message || `Lỗi máy chủ (${response.status})`)
        }
        if (!response.body) return

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ""

        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split("\n\n")
          buffer = lines.pop() || ""

          for (const chunk of lines) {
            const eventMatch = chunk.match(/^event:\s*(\w+)/m)
            const dataMatch = chunk.match(/^data:\s*(.*)/m)
            if (eventMatch && dataMatch) {
              const event = eventMatch[1]
              const dataText = dataMatch[1]
              try {
                const parsed = JSON.parse(dataText)
                if (event === "token") callbacks.onToken?.(parsed.token || parsed)
                else if (event === "tool_start") callbacks.onToolStart?.(parsed.tool)
                else if (event === "tool_end") callbacks.onToolEnd?.(parsed.tool)
                else if (event === "proposal") callbacks.onProposal?.(parsed)
                else if (event === "done") callbacks.onDone?.()
                else if (event === "error") callbacks.onError?.(parsed)
              } catch {
                if (event === "token") callbacks.onToken?.(dataText)
              }
            }
          }
        }
        callbacks.onDone?.()
      })
      .catch((err) => {
        callbacks.onError?.(err)
      })
  },
}
