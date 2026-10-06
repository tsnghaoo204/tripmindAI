import { request } from "../client"
import type { ChecklistItem } from "../types"

export interface CreateChecklistItemInput {
  title: string
  category: string
  kind: "PACK" | "TODO"
  dueDate?: string
}

export interface ChecklistSuggestionsResponse {
  items: Array<{
    title: string
    category: string
    kind: "PACK" | "TODO"
    dueDate?: string
    reason: string
  }>
  skippedRules?: string[]
}

export const checklistApi = {
  getChecklist: async (tripId: number): Promise<ChecklistItem[]> => {
    return request<ChecklistItem[]>(`/api/trips/${tripId}/checklist`)
  },

  createItem: async (tripId: number, data: CreateChecklistItemInput): Promise<ChecklistItem> => {
    return request<ChecklistItem>(`/api/trips/${tripId}/checklist`, {
      method: "POST",
      data,
    })
  },

  updateItem: async (
    itemId: number,
    data: { isCompleted?: boolean; title?: string },
  ): Promise<ChecklistItem> => {
    return request<ChecklistItem>(`/api/checklist-items/${itemId}`, {
      method: "PATCH",
      data,
    })
  },

  deleteItem: async (itemId: number): Promise<void> => {
    return request<void>(`/api/checklist-items/${itemId}`, {
      method: "DELETE",
    })
  },

  getSuggestions: async (tripId: number): Promise<ChecklistSuggestionsResponse> => {
    return request<ChecklistSuggestionsResponse>(`/api/trips/${tripId}/checklist/suggestions`)
  },
}
