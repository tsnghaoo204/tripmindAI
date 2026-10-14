import { describe, expect, it } from "vitest"
import { apiErrorFromBody, skipReasonLabel } from "./errors"

describe("apiErrorFromBody", () => {
  it("keeps the backend message, code and status", () => {
    const err = apiErrorFromBody(
      { success: false, code: "CURRENCY_CHANGE_BLOCKED", message: "Không thể đổi tiền tệ" },
      422,
    )
    expect(err).toMatchObject({ message: "Không thể đổi tiền tệ", code: "CURRENCY_CHANGE_BLOCKED", status: 422 })
  })

  it("explains rate limits with the retry delay", () => {
    expect(apiErrorFromBody({ details: { retryAfterSeconds: 30 } }, 429).message).toContain("30 giây")
  })

  it("falls back to a status message when the body is empty", () => {
    expect(apiErrorFromBody(undefined, 500).message).toBe("Lỗi máy chủ (500)")
  })
})

describe("skipReasonLabel", () => {
  it("translates known reasons and passes through unknown ones", () => {
    expect(skipReasonLabel("PLACE_UNAVAILABLE")).toBe("địa điểm không còn khả dụng")
    expect(skipReasonLabel("SOMETHING_ELSE")).toBe("SOMETHING_ELSE")
  })
})
