import { afterEach, describe, expect, it, vi } from "vitest"
import { type ChatCallbacks, aiApi } from "./ai-api"

function sseResponse(chunks: string[], status = 200): Response {
  const encoder = new TextEncoder()
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const c of chunks) controller.enqueue(encoder.encode(c))
      controller.close()
    },
  })
  return new Response(body, { status, headers: { "Content-Type": "text/event-stream" } })
}

function runChat(response: Response) {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response))
  const tokens: string[] = []
  const callbacks = {
    onToken: (t: string) => tokens.push(t),
    onProposal: vi.fn(),
    onDone: vi.fn(),
    onError: vi.fn(),
  } satisfies ChatCallbacks
  return new Promise<{ tokens: string[]; callbacks: typeof callbacks }>((resolve) => {
    const settle = () => setTimeout(() => resolve({ tokens, callbacks }), 0)
    callbacks.onDone.mockImplementation(settle)
    callbacks.onError.mockImplementation(settle)
    aiApi.streamChat(1, { message: "hi" }, callbacks)
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe("aiApi.streamChat", () => {
  it("reads token text from the {text} payload", async () => {
    const { tokens } = await runChat(
      sseResponse(['event: token\ndata: {"text":"Xin "}\n\n', 'event: token\ndata: {"text":"chào"}\n\n']),
    )
    expect(tokens.join("")).toBe("Xin chào")
  })

  it("fires onDone exactly once with the conversation id", async () => {
    const { callbacks } = await runChat(
      sseResponse([
        'event: proposal\ndata: {"proposalId":7,"summary":"s","estimatedCostDelta":0,"travelTimeDelta":5,"changes":2}\n\n',
        'event: done\ndata: {"messageId":3,"conversationId":42}\n\n',
      ]),
    )
    expect(callbacks.onProposal).toHaveBeenCalledWith(expect.objectContaining({ proposalId: 7, changes: 2 }))
    expect(callbacks.onDone).toHaveBeenCalledTimes(1)
    expect(callbacks.onDone).toHaveBeenCalledWith({ messageId: 3, conversationId: 42 })
    expect(callbacks.onError).not.toHaveBeenCalled()
  })

  it("still finishes once when the stream ends without a done event", async () => {
    const { callbacks } = await runChat(sseResponse(['event: token\ndata: {"text":"x"}\n\n']))
    expect(callbacks.onDone).toHaveBeenCalledTimes(1)
    expect(callbacks.onDone).toHaveBeenCalledWith(null)
  })

  it("reports an error event once and does not call onDone", async () => {
    const { callbacks } = await runChat(
      sseResponse(['event: error\ndata: {"code":"AI_TIMEOUT","message":"Hết thời gian"}\n\n']),
    )
    expect(callbacks.onError).toHaveBeenCalledTimes(1)
    expect(callbacks.onError.mock.calls[0][0]).toMatchObject({ message: "Hết thời gian", code: "AI_TIMEOUT" })
    expect(callbacks.onDone).not.toHaveBeenCalled()
  })

  it("surfaces the backend message for non-2xx responses", async () => {
    const res = new Response(JSON.stringify({ success: false, message: "Không tìm thấy chuyến đi" }), { status: 404 })
    const { callbacks } = await runChat(res)
    expect(callbacks.onError.mock.calls[0][0]).toMatchObject({ message: "Không tìm thấy chuyến đi", status: 404 })
  })
})
