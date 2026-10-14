import { describe, expect, it } from "vitest"
import { type SseEvent, createSseParser } from "./sse"

function parseAll(chunks: string[]): SseEvent[] {
  const events: SseEvent[] = []
  const parser = createSseParser((e) => events.push(e))
  for (const c of chunks) parser.push(c)
  parser.end()
  return events
}

describe("createSseParser", () => {
  it("parses named events with JSON data", () => {
    expect(parseAll(['event:token\ndata:{"text":"Xin"}\n\n'])).toEqual([{ event: "token", data: '{"text":"Xin"}' }])
  })

  it("joins multi-line data fields with newlines", () => {
    const events = parseAll(["event: done\ndata: line one\ndata: line two\n\n"])
    expect(events).toEqual([{ event: "done", data: "line one\nline two" }])
  })

  it("handles events split across chunks and CRLF line endings", () => {
    const events = parseAll(["event: tok", 'en\r\ndata: {"te', 'xt":"a"}\r\n\r\nevent: done\r\ndata: {}\r\n\r\n'])
    expect(events).toEqual([
      { event: "token", data: '{"text":"a"}' },
      { event: "done", data: "{}" },
    ])
  })

  it("ignores comments and defaults the event name to message", () => {
    expect(parseAll([": keep-alive\n\ndata: hi\n\n"])).toEqual([{ event: "message", data: "hi" }])
  })

  it("flushes a trailing event without a blank line on end", () => {
    expect(parseAll(["event: done\ndata: {}"])).toEqual([{ event: "done", data: "{}" }])
  })
})
