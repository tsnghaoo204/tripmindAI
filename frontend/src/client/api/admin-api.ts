import { request } from "../client"

export interface ToolExecutionAudit {
  id: number
  userId: number
  userName?: string
  toolName: string
  status: "OK" | "FAILED" | "TIMEOUT"
  arguments: string
  result: string
  executionTimeMs: number
  createdAt: string
}

export interface AiUsageMetrics {
  totalCalls: number
  totalPromptTokens: number
  totalCompletionTokens: number
  errorRate: number
  topTools: Array<{ toolName: string; count: number }>
}

export const adminApi = {
  getToolExecutions: async (params?: { page?: number; size?: number; toolName?: string }): Promise<any> => {
    const q = new URLSearchParams()
    if (params?.page) q.set("page", String(params.page))
    if (params?.size) q.set("size", String(params.size))
    if (params?.toolName) q.set("toolName", params.toolName)
    return request<any>(`/api/admin/tool-executions?${q.toString()}`)
  },

  getAiUsage: async (): Promise<AiUsageMetrics> => {
    return request<AiUsageMetrics>("/api/admin/ai-usage")
  },

  getUsers: async (page = 0, size = 20): Promise<any> => {
    return request<any>(`/api/admin/users?page=${page}&size=${size}`)
  },

  createUser: async (data: { email: string; password: string; name: string; role: "USER" | "ADMIN" }): Promise<any> => {
    return request<any>("/api/admin/users", {
      method: "POST",
      data,
    })
  },

  updateUser: async (
    userId: number,
    data: { name?: string; role?: "USER" | "ADMIN"; active?: boolean; password?: string },
  ): Promise<any> => {
    return request<any>(`/api/admin/users/${userId}`, {
      method: "PUT",
      data,
    })
  },

  deleteUser: async (userId: number): Promise<void> => {
    return request<void>(`/api/admin/users/${userId}`, {
      method: "DELETE",
    })
  },

  getTrips: async (page = 0, size = 20): Promise<any> => {
    return request<any>(`/api/admin/trips?page=${page}&size=${size}`)
  },

  updateTrip: async (
    tripId: number,
    data: { name?: string; phase?: string; budget?: number },
  ): Promise<any> => {
    return request<any>(`/api/admin/trips/${tripId}`, {
      method: "PUT",
      data,
    })
  },

  deleteTrip: async (tripId: number): Promise<void> => {
    return request<void>(`/api/admin/trips/${tripId}`, {
      method: "DELETE",
    })
  },
}
