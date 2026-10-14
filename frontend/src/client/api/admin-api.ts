import { request } from "../client"
import type {
  AdminTripRow,
  AdminUserRow,
  AiUsageMetrics,
  DestinationKnowledge,
  KnowledgeCategory,
  KnowledgeRequest,
  PageResponse,
  ToolExecution,
  TripPhase,
  UserRole,
} from "../types"

export const adminApi = {
  getToolExecutions: (params?: { page?: number; size?: number; tool?: string }) => {
    const q = new URLSearchParams()
    if (params?.page !== undefined) q.set("page", String(params.page))
    if (params?.size !== undefined) q.set("size", String(params.size))
    if (params?.tool) q.set("tool", params.tool)
    return request<PageResponse<ToolExecution>>(`/api/admin/tool-executions?${q.toString()}`)
  },

  getAiUsage: () => request<AiUsageMetrics>("/api/admin/ai-usage"),

  getUsers: (page = 0, size = 20) => request<PageResponse<AdminUserRow>>(`/api/admin/users?page=${page}&size=${size}`),

  createUser: (data: { email: string; password: string; name: string; role: UserRole }) =>
    request<AdminUserRow>("/api/admin/users", { method: "POST", data }),

  updateUser: (userId: number, data: { name?: string; role?: UserRole; active?: boolean; password?: string }) =>
    request<AdminUserRow>(`/api/admin/users/${userId}`, { method: "PUT", data }),

  deleteUser: (userId: number) => request<void>(`/api/admin/users/${userId}`, { method: "DELETE" }),

  getTrips: (page = 0, size = 20) => request<PageResponse<AdminTripRow>>(`/api/admin/trips?page=${page}&size=${size}`),

  updateTrip: (tripId: number, data: { name?: string; phase?: TripPhase | null; budget?: number }) =>
    request<unknown>(`/api/admin/trips/${tripId}`, { method: "PUT", data }),

  deleteTrip: (tripId: number) => request<void>(`/api/admin/trips/${tripId}`, { method: "DELETE" }),

  getKnowledgeList: (params?: {
    destination?: string
    category?: KnowledgeCategory
    search?: string
    page?: number
    size?: number
  }) => {
    const q = new URLSearchParams()
    if (params?.destination) q.set("destination", params.destination)
    if (params?.category) q.set("category", params.category)
    if (params?.search) q.set("search", params.search)
    if (params?.page !== undefined) q.set("page", String(params.page))
    if (params?.size !== undefined) q.set("size", String(params.size))
    return request<PageResponse<DestinationKnowledge>>(`/api/admin/knowledge?${q.toString()}`)
  },

  getKnowledgeDestinations: () => request<string[]>("/api/admin/knowledge/destinations"),

  createKnowledge: (data: KnowledgeRequest) =>
    request<DestinationKnowledge>("/api/admin/knowledge", { method: "POST", data }),

  updateKnowledge: (id: number, data: KnowledgeRequest) =>
    request<DestinationKnowledge>(`/api/admin/knowledge/${id}`, { method: "PUT", data }),

  deleteKnowledge: (id: number) => request<void>(`/api/admin/knowledge/${id}`, { method: "DELETE" }),

  toggleKnowledgeActive: (id: number) =>
    request<DestinationKnowledge>(`/api/admin/knowledge/${id}/toggle`, { method: "PATCH" }),
}
