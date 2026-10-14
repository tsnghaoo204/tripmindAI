import type { ChatProposalEvent } from "@/client/api/ai-api"
import type { ChatMessage, ProposalStatus } from "@/client/types"

export interface ChatProposal {
  proposalId: number
  summary: string
  estimatedCostDelta: number | null
  travelTimeDelta: number | null
  changeCount: number
  status: ProposalStatus
}

export interface ChatMessageItem {
  id: string
  role: "user" | "assistant"
  text: string
  proposals: ChatProposal[]
  streaming?: boolean
}

export function newMessageId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

/** Proposals arrive from the stream without a status; a fresh one is always pending. */
export function proposalFromEvent(evt: ChatProposalEvent): ChatProposal {
  return {
    proposalId: evt.proposalId,
    summary: evt.summary,
    estimatedCostDelta: typeof evt.estimatedCostDelta === "number" ? evt.estimatedCostDelta : null,
    travelTimeDelta: typeof evt.travelTimeDelta === "number" ? evt.travelTimeDelta : null,
    changeCount: typeof evt.changes === "number" ? evt.changes : 0,
    status: "PENDING",
  }
}

function updateMessage(
  messages: ChatMessageItem[],
  id: string,
  fn: (m: ChatMessageItem) => ChatMessageItem,
): ChatMessageItem[] {
  return messages.map((m) => (m.id === id ? fn(m) : m))
}

export function appendToken(messages: ChatMessageItem[], id: string, text: string): ChatMessageItem[] {
  return updateMessage(messages, id, (m) => ({ ...m, text: m.text + text }))
}

export function addProposal(messages: ChatMessageItem[], id: string, proposal: ChatProposal): ChatMessageItem[] {
  return updateMessage(messages, id, (m) =>
    m.proposals.some((p) => p.proposalId === proposal.proposalId) ? m : { ...m, proposals: [...m.proposals, proposal] },
  )
}

export function setProposalStatus(
  messages: ChatMessageItem[],
  proposalId: number,
  status: ProposalStatus,
): ChatMessageItem[] {
  return messages.map((m) =>
    m.proposals.some((p) => p.proposalId === proposalId)
      ? { ...m, proposals: m.proposals.map((p) => (p.proposalId === proposalId ? { ...p, status } : p)) }
      : m,
  )
}

/** Ends streaming; drops the assistant bubble when it got neither text nor proposals. */
export function finishStreaming(messages: ChatMessageItem[], id: string, fallbackText?: string): ChatMessageItem[] {
  return messages.flatMap((m) => {
    if (m.id !== id) return [m]
    if (!m.text && m.proposals.length === 0) {
      return fallbackText ? [{ ...m, text: fallbackText, streaming: false }] : []
    }
    return [{ ...m, streaming: false }]
  })
}

export function messagesFromHistory(history: ChatMessage[]): ChatMessageItem[] {
  return history
    .filter((m) => (m.role === "USER" || m.role === "ASSISTANT") && m.content)
    .map((m) => ({
      id: `history-${m.id}`,
      role: m.role === "USER" ? "user" : "assistant",
      text: m.content,
      proposals: [],
    }))
}
