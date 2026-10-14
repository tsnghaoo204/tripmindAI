// Client-side mirrors of backend validation limits
export const MIN_PASSWORD_LENGTH = 8
export const MAX_BLOG_PHOTOS = 10
export const PROMPT_MAX_LENGTH = 1000

export function isHttpsUrl(value: string): boolean {
  try {
    return new URL(value).protocol === "https:"
  } catch {
    return false
  }
}

/** Returns an error message for invalid blog photo URLs, or null when valid. */
export function validatePhotoUrls(urls: string[]): string | null {
  if (urls.length > MAX_BLOG_PHOTOS) return `Tối đa ${MAX_BLOG_PHOTOS} link hình ảnh`
  const bad = urls.find((u) => !isHttpsUrl(u))
  return bad ? `Link hình ảnh phải bắt đầu bằng https://: ${bad}` : null
}

/** avatarUrl must be https, or an empty string to clear it. */
export function validateAvatarUrl(url: string): string | null {
  return url === "" || isHttpsUrl(url) ? null : "Ảnh đại diện phải là link https://"
}
