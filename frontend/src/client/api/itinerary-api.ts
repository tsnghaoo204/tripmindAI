import { request } from "../client"
import type { Activity, ItineraryDay } from "../types"

export interface ItineraryResponse {
  tripId: number
  days: ItineraryDay[]
}

export interface CreateActivityInput {
  dayNumber: number
  title: string
  activityType: string
  placeId?: number
  placeExternalId?: string
  startTime?: string
  endTime?: string
  estimatedCost?: number
  notes?: string
}

export interface UpdateActivityInput {
  dayNumber?: number
  title?: string
  activityType?: string
  startTime?: string
  endTime?: string
  estimatedCost?: number
  notes?: string
  status?: "PLANNED" | "COMPLETED" | "SKIPPED"
  skipReason?: string
}

export interface CostEstimateResponse {
  suggested?: number
  min?: number
  max?: number
  currency?: string
  travelers?: number
  priceLevel?: number
  basis?: string
  source?: string
  reason?: string
}

export const itineraryApi = {
  getItinerary: async (tripId: number): Promise<ItineraryResponse> => {
    return request<ItineraryResponse>(`/api/trips/${tripId}/itinerary`)
  },

  addActivity: async (tripId: number, data: CreateActivityInput): Promise<Activity> => {
    return request<Activity>(`/api/trips/${tripId}/itinerary/activities`, {
      method: "POST",
      data,
    })
  },

  updateActivity: async (activityId: number, data: UpdateActivityInput): Promise<Activity> => {
    return request<Activity>(`/api/activities/${activityId}`, {
      method: "PUT",
      data,
    })
  },

  deleteActivity: async (activityId: number): Promise<void> => {
    return request<void>(`/api/activities/${activityId}`, {
      method: "DELETE",
    })
  },

  reorderDay: async (dayId: number, activityIds: number[]): Promise<void> => {
    return request<void>(`/api/itinerary-days/${dayId}/reorder`, {
      method: "PUT",
      data: { activityIds },
    })
  },

  getCostEstimate: async (
    tripId: number,
    params: { placeId?: number; priceLevel?: number; activityType?: string },
  ): Promise<CostEstimateResponse> => {
    const q = new URLSearchParams()
    if (params.placeId != null) q.set("placeId", String(params.placeId))
    if (params.priceLevel != null) q.set("priceLevel", String(params.priceLevel))
    if (params.activityType) q.set("activityType", params.activityType)
    return request<CostEstimateResponse>(`/api/trips/${tripId}/cost-estimate?${q.toString()}`)
  },

  getAlternatives: async (activityId: number): Promise<any[]> => {
    return request<any[]>(`/api/activities/${activityId}/alternatives`)
  },

  selectAlternative: async (activityId: number, altId: number): Promise<Activity> => {
    return request<Activity>(`/api/activities/${activityId}/alternatives/${altId}/select`, {
      method: "PUT",
    })
  },
}
