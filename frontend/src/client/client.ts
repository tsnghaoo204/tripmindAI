import { APP_CONFIG } from "@/config/app.config"
import { getToken, logout } from "@/lib/auth-storage"
import axios, { type AxiosRequestConfig, isAxiosError } from "axios"
import { ApiError, NETWORK_ERROR_MESSAGE, apiErrorFromBody } from "./errors"
import type { ApiResponse } from "./types"

export const apiClient = axios.create({
  baseURL: APP_CONFIG.apiBaseUrl,
  headers: {
    "Content-Type": "application/json",
  },
})

apiClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!isAxiosError(error)) return Promise.reject(error)
    if (error.code === "ERR_CANCELED") return Promise.reject(error)
    const status = error.response?.status
    // Session expired: only when we actually sent a token (login failures stay on the form)
    if (status === 401 && error.config?.headers?.Authorization) {
      logout()
    }
    if (!error.response) {
      return Promise.reject(new ApiError(NETWORK_ERROR_MESSAGE))
    }
    let body = error.response.data
    if (body instanceof Blob) {
      body = await body
        .text()
        .then((t) => JSON.parse(t))
        .catch(() => undefined)
    }
    return Promise.reject(apiErrorFromBody(body, status))
  },
)

export async function request<T>(url: string, options?: AxiosRequestConfig): Promise<T> {
  const response = await apiClient(url, options)
  const apiRes = response.data as ApiResponse<T>
  if (apiRes.success === false) {
    throw apiErrorFromBody(apiRes, response.status)
  }
  return apiRes.data as T
}

/** Downloads a binary endpoint with the auth header and saves it via a temporary link. */
export async function downloadFile(url: string, fallbackName: string): Promise<void> {
  const res = await apiClient.get<Blob>(url, { responseType: "blob" })
  const disposition = String(res.headers["content-disposition"] ?? "")
  const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i)
  const filename = match ? decodeURIComponent(match[1]) : fallbackName
  const href = URL.createObjectURL(res.data)
  const a = document.createElement("a")
  a.href = href
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(href)
}
