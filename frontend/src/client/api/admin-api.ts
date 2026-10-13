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

  getKnowledgeList: async (params?: {
    destination?: string
    category?: string
    search?: string
    page?: number
    size?: number
  }): Promise<{ content: import("../types").DestinationKnowledge[]; totalElements: number; totalPages: number; number: number }> => {
    const q = new URLSearchParams()
    if (params?.destination) q.set("destination", params.destination)
    if (params?.category) q.set("category", params.category)
    if (params?.search) q.set("search", params.search)
    if (params?.page !== undefined) q.set("page", String(params.page))
    if (params?.size !== undefined) q.set("size", String(params.size))
    return request<any>(`/api/admin/knowledge?${q.toString()}`)
  },

  getKnowledgeDestinations: async (): Promise<string[]> => {
    return request<string[]>("/api/admin/knowledge/destinations")
  },

  createKnowledge: async (data: import("../types").KnowledgeRequest): Promise<import("../types").DestinationKnowledge> => {
    return request<import("../types").DestinationKnowledge>("/api/admin/knowledge", {
      method: "POST",
      data,
    })
  },

  updateKnowledge: async (id: number, data: import("../types").KnowledgeRequest): Promise<import("../types").DestinationKnowledge> => {
    return request<import("../types").DestinationKnowledge>(`/api/admin/knowledge/${id}`, {
      method: "PUT",
      data,
    })
  },

  deleteKnowledge: async (id: number): Promise<void> => {
    return request<void>(`/api/admin/knowledge/${id}`, {
      method: "DELETE",
    })
  },

  toggleKnowledgeActive: async (id: number): Promise<import("../types").DestinationKnowledge> => {
    return request<import("../types").DestinationKnowledge>(`/api/admin/knowledge/${id}/toggle`, {
      method: "PATCH",
    })
  },
}

