import { describe, expect, it } from "vitest"
import {
  type ChatMessageItem,
  addProposal,
  appendToken,
  finishStreaming,
  proposalFromEvent,
  setProposalStatus,
} from "./chat-state"

const event = { proposalId: 9, summary: "Đổi quán ăn", estimatedCostDelta: -50000, travelTimeDelta: 12, changes: 3 }

function streaming(): ChatMessageItem[] {
  return [{ id: "a", role: "assistant", text: "", proposals: [], streaming: true }]
}

describe("chat proposal handling", () => {
  it("maps the SSE proposal payload to a pending proposal with a change count", () => {
    expect(proposalFromEvent(event)).toEqual({
      proposalId: 9,
      summary: "Đổi quán ăn",
      estimatedCostDelta: -50000,
      travelTimeDelta: 12,
      changeCount: 3,
      status: "PENDING",
    })
  })

  it("keeps a proposal that arrives before any token and dedupes repeats", () => {
    let msgs = addProposal(streaming(), "a", proposalFromEvent(event))
    msgs = addProposal(msgs, "a", proposalFromEvent(event))
    msgs = appendToken(msgs, "a", "Xong")
    msgs = finishStreaming(msgs, "a")
    expect(msgs).toHaveLength(1)
    expect(msgs[0].text).toBe("Xong")
    expect(msgs[0].proposals).toHaveLength(1)
    expect(msgs[0].streaming).toBe(false)
  })

  it("updates status so an applied proposal cannot be applied again", () => {
    const msgs = setProposalStatus(addProposal(streaming(), "a", proposalFromEvent(event)), 9, "APPLIED")
    expect(msgs[0].proposals[0].status).toBe("APPLIED")
  })

  it("drops an empty assistant bubble unless a fallback text is given", () => {
    expect(finishStreaming(streaming(), "a")).toHaveLength(0)
    expect(finishStreaming(streaming(), "a", "fallback")[0].text).toBe("fallback")
  })
})
