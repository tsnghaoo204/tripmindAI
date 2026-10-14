import { request } from "../client"
import type {
  Activity,
  ActivityAlternative,
  ActivityStatus,
  ActivityType,
  CostSource,
  Itinerary,
  SkipReason,
} from "../types"

export type ItineraryResponse = Itinerary

export interface CreateActivityInput {
  dayNumber: number
  title: string
  activityType: ActivityType
  placeId?: number
  placeExternalId?: string
  startTime?: string
  endTime?: string
  estimatedCost?: number
  estimatedCostSource?: CostSource
  notes?: string
}

export interface UpdateActivityInput {
  title?: string
  activityType?: ActivityType
  startTime?: string
  endTime?: string
  estimatedCost?: number
  notes?: string
  status?: ActivityStatus
  skipReason?: SkipReason
}

export interface CostEstimateResponse {
  suggested?: number | null
  min?: number | null
  max?: number | null
  currency?: string
  travelers?: number
  priceLevel?: number | null
  basis?: string | null
  source?: CostSource | null
  reason?: string | null
}

export const itineraryApi = {
  getItinerary: (tripId: number) => request<Itinerary>(`/api/trips/${tripId}/itinerary`),

  addActivity: (tripId: number, data: CreateActivityInput) =>
    request<Activity>(`/api/trips/${tripId}/itinerary/activities`, { method: "POST", data }),

  updateActivity: (activityId: number, data: UpdateActivityInput) =>
    request<Activity>(`/api/activities/${activityId}`, { method: "PUT", data }),

  deleteActivity: (activityId: number) => request<void>(`/api/activities/${activityId}`, { method: "DELETE" }),

  reorderDay: (dayId: number, activityIds: number[]) =>
    request<void>(`/api/itinerary-days/${dayId}/reorder`, { method: "PUT", data: { activityIds } }),

  getCostEstimate: (tripId: number, params: { placeId?: number; priceLevel?: number; activityType?: ActivityType }) => {
    const q = new URLSearchParams()
    if (params.placeId != null) q.set("placeId", String(params.placeId))
    if (params.priceLevel != null) q.set("priceLevel", String(params.priceLevel))
    if (params.activityType) q.set("activityType", params.activityType)
    return request<CostEstimateResponse>(`/api/trips/${tripId}/cost-estimate?${q.toString()}`)
  },

  getAlternatives: (activityId: number) => request<ActivityAlternative[]>(`/api/activities/${activityId}/alternatives`),

  selectAlternative: (activityId: number, altId: number) =>
    request<Activity>(`/api/activities/${activityId}/alternatives/${altId}/select`, { method: "PUT" }),
}
