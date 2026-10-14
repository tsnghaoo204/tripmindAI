import { request } from "../client"
import type { ChecklistCategory, ChecklistItem, ChecklistKind } from "../types"

export interface CreateChecklistItemInput {
  title: string
  kind: ChecklistKind
  category?: ChecklistCategory | null
  dueDate?: string | null
  source?: "USER" | "SUGGESTED" | "AI"
  reason?: string | null
}

export interface ChecklistSuggestionsResponse {
  items: ChecklistItem[]
  skippedRules?: string[]
}

export const checklistApi = {
  getChecklist: (tripId: number) => request<ChecklistItem[]>(`/api/trips/${tripId}/checklist`),

  createItems: (tripId: number, data: CreateChecklistItemInput[]) =>
    request<ChecklistItem[]>(`/api/trips/${tripId}/checklist`, { method: "POST", data }),

  updateItem: (
    itemId: number,
    data: { done?: boolean; title?: string; category?: ChecklistCategory; dueDate?: string },
  ) => request<ChecklistItem>(`/api/checklist-items/${itemId}`, { method: "PATCH", data }),

  deleteItem: (itemId: number) => request<void>(`/api/checklist-items/${itemId}`, { method: "DELETE" }),

  getSuggestions: (tripId: number) =>
    request<ChecklistSuggestionsResponse>(`/api/trips/${tripId}/checklist/suggestions`),
}
