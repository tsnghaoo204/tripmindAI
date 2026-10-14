import type { ApiResponse } from "./types"

export const NETWORK_ERROR_MESSAGE = "Không thể kết nối đến máy chủ"
export const DEFAULT_ERROR_MESSAGE = "Đã xảy ra lỗi"

export class ApiError extends Error {
  readonly status?: number
  readonly code?: string
  readonly details?: unknown

  constructor(message: string, opts: { status?: number; code?: string; details?: unknown } = {}) {
    super(message)
    this.name = "ApiError"
    this.status = opts.status
    this.code = opts.code
    this.details = opts.details
  }
}

/** Builds an ApiError from a backend error body (ApiResponse) and HTTP status. */
export function apiErrorFromBody(body: unknown, status?: number): ApiError {
  const res = (body && typeof body === "object" ? body : {}) as Partial<ApiResponse<unknown>>
  const opts = { status, code: res.code, details: res.details }
  if (status === 429) return new ApiError(rateLimitMessage(res.details), opts)
  if (status === 413) return new ApiError("Dữ liệu gửi lên quá lớn (tối đa 1MB)", opts)
  if (res.code === "AI_TIMEOUT" || (status === 504 && !res.message)) {
    return new ApiError("Trợ lý AI phản hồi quá lâu, vui lòng thử lại sau", opts)
  }
  const fallback = status ? `Lỗi máy chủ (${status})` : DEFAULT_ERROR_MESSAGE
  return new ApiError(res.message || fallback, opts)
}

export function rateLimitMessage(details: unknown): string {
  const retry =
    details && typeof details === "object" && "retryAfterSeconds" in details
      ? Number((details as { retryAfterSeconds: unknown }).retryAfterSeconds)
      : Number.NaN
  return Number.isFinite(retry) && retry > 0
    ? `Bạn thao tác quá nhiều lần, vui lòng thử lại sau ${Math.ceil(retry)} giây`
    : "Bạn thao tác quá nhiều lần, vui lòng thử lại sau ít phút"
}

export function getErrorMessage(err: unknown, fallback = DEFAULT_ERROR_MESSAGE): string {
  if (err instanceof Error && err.message) return err.message
  if (err && typeof err === "object" && "message" in err && typeof err.message === "string") return err.message
  return fallback
}

export function isForbidden(err: unknown): boolean {
  return err instanceof ApiError && err.status === 403
}

const SKIP_REASON_LABEL: Record<string, string> = {
  ACTIVITY_HAS_LINKED_DATA: "hoạt động đã có dữ liệu liên quan (chi tiêu, chỉnh sửa)",
  PLACE_UNAVAILABLE: "địa điểm không còn khả dụng",
}

/** Human-readable reason for a change skipped during apply/undo. */
export function skipReasonLabel(reason: string): string {
  return SKIP_REASON_LABEL[reason] ?? reason
}
