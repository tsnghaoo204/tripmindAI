import { apiUrl } from "@/config/app.config"
import { getToken, logout } from "@/lib/auth-storage"
import { ApiError, NETWORK_ERROR_MESSAGE, apiErrorFromBody } from "./errors"

export interface SseEvent {
  event: string
  data: string
}

/** Incremental text/event-stream parser; feed raw chunks, receive complete events. */
export function createSseParser(onEvent: (evt: SseEvent) => void) {
  let buffer = ""
  let eventName = ""
  let dataLines: string[] = []

  const dispatch = () => {
    if (dataLines.length > 0) {
      onEvent({ event: eventName || "message", data: dataLines.join("\n") })
    }
    eventName = ""
    dataLines = []
  }

  const processLine = (line: string) => {
    if (line === "") return dispatch()
    if (line.startsWith(":")) return
    const idx = line.indexOf(":")
    const field = idx === -1 ? line : line.slice(0, idx)
    let value = idx === -1 ? "" : line.slice(idx + 1)
    if (value.startsWith(" ")) value = value.slice(1)
    if (field === "event") eventName = value
    else if (field === "data") dataLines.push(value)
  }

  return {
    push(chunk: string) {
      buffer += chunk
      const lines = buffer.split(/\r\n|\r|\n/)
      buffer = lines.pop() ?? ""
      for (const line of lines) processLine(line)
    },
    /** Flushes a trailing event that was not followed by a blank line. */
    end() {
      if (buffer) processLine(buffer)
      buffer = ""
      dispatch()
    },
  }
}

export interface SseRequest {
  path: string
  method?: "GET" | "POST"
  body?: unknown
  signal: AbortSignal
  onEvent: (evt: SseEvent) => void
}

/** Opens an authenticated SSE stream via fetch; resolves when the stream ends. */
export async function openSseStream({ path, method = "GET", body, signal, onEvent }: SseRequest): Promise<void> {
  const token = getToken()
  let response: Response
  try {
    response = await fetch(apiUrl(path), {
      method,
      headers: {
        Accept: "text/event-stream",
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    })
  } catch (err) {
    if (signal.aborted) throw err
    throw new ApiError(NETWORK_ERROR_MESSAGE)
  }

  if (!response.ok) {
    if (response.status === 401 && token) logout()
    const errJson = await response.json().catch(() => undefined)
    throw apiErrorFromBody(errJson, response.status)
  }
  if (!response.body) return

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  const parser = createSseParser(onEvent)
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    parser.push(decoder.decode(value, { stream: true }))
  }
  parser.push(decoder.decode())
  parser.end()
}

export function parseJson<T>(data: string): T | null {
  try {
    return JSON.parse(data) as T
  } catch {
    return null
  }
}

export function isAbortError(err: unknown): boolean {
  return err instanceof DOMException ? err.name === "AbortError" : (err as { name?: string })?.name === "AbortError"
}
