import axios from "axios"
import type { ApiResponse } from "./types"

const API_BASE_URL = import.meta.env.VITE_API_URL || ""

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token")
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("access_token")
      if (window.location.pathname !== "/login") {
        window.location.href = "/login"
      }
    }
    return Promise.reject(error)
  },
)

export async function request<T>(url: string, options?: Parameters<typeof apiClient>[0]): Promise<T> {
  const response = await apiClient(url, options)
  const apiRes = response.data as ApiResponse<T>
  if (apiRes.success === false) {
    throw new Error(apiRes.message || "Đã xảy ra lỗi")
  }
  return apiRes.data as T
}
