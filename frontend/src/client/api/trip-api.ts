import { downloadFile, request } from "../client"
import type {
  BudgetPreference,
  Destination,
  DuplicateTripResponse,
  GroupProfile,
  TravelPlan,
  TravelStyle,
  Trip,
  TripPhase,
} from "../types"

export interface DestinationInput {
  provider?: string
  externalId?: string
  name: string
  country: string
  latitude?: number | null
  longitude?: number | null
  timezone?: string
  formattedAddress?: string
}

export interface CreateTripInput {
  destinationId?: number
  destinationPlaceId?: string
  destinationData?: DestinationInput
  name: string
  startDate: string
  endDate: string
  travelers: number
  budget?: number | null
  currency?: string
  travelStyle?: TravelStyle
  budgetPreference?: BudgetPreference
  preferences?: string[]
  groupProfile?: GroupProfile
  travelPlan?: TravelPlan
}

export type UpdateTripInput = Partial<
  Omit<CreateTripInput, "destinationId" | "destinationPlaceId" | "destinationData">
> & {
  confirmDropDays?: boolean
}

export const tripApi = {
  getTrips: (status?: "upcoming" | "past") => {
    const params = status ? `?status=${status}` : ""
    return request<Trip[]>(`/api/trips${params}`)
  },
  getTripById: (tripId: number) => {
    return request<Trip>(`/api/trips/${tripId}`)
  },
  createTrip: (data: CreateTripInput) => {
    return request<Trip>("/api/trips", { method: "POST", data })
  },
  updateTrip: (tripId: number, data: UpdateTripInput) => {
    return request<Trip>(`/api/trips/${tripId}`, { method: "PUT", data })
  },
  updateTripPhase: (tripId: number, phase: TripPhase | null) => {
    return request<Trip>(`/api/trips/${tripId}/phase`, { method: "PUT", data: { phase } })
  },
  duplicateTrip: (tripId: number, data: { name?: string; startDate: string }) => {
    return request<DuplicateTripResponse>(`/api/trips/${tripId}/duplicate`, { method: "POST", data })
  },
  deleteTrip: (tripId: number) => {
    return request<void>(`/api/trips/${tripId}`, { method: "DELETE" })
  },
  exportIcs: (tripId: number) => {
    return downloadFile(`/api/trips/${tripId}/export.ics`, `trip-${tripId}.ics`)
  },
  parsePrompt: (prompt: string) => {
    return request<Record<string, any>>("/api/trips/parse-prompt", { method: "POST", data: { prompt } })
  },
  getDestinations: (query?: string) => {
    const q = query ? `?query=${encodeURIComponent(query)}` : ""
    return request<Destination[]>(`/api/destinations${q}`)
  },
}
