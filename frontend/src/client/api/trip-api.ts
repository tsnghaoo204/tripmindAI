import { request } from "../client"
import type { Trip } from "../types"

export const tripApi = {
  getTrips: (status?: "upcoming" | "past") => {
    const params = status ? `?status=${status}` : ""
    return request<Trip[]>(`/api/trips${params}`)
  },
  getTripById: (tripId: number) => {
    return request<Trip>(`/api/trips/${tripId}`)
  },
  createTrip: (data: Partial<Trip>) => {
    return request<Trip>("/api/trips", { method: "POST", data })
  },
  updateTrip: (tripId: number, data: any) => {
    return request<Trip>(`/api/trips/${tripId}`, { method: "PUT", data })
  },
  updateTripPhase: (tripId: number, phase: "BEFORE" | "DURING" | "AFTER" | null) => {
    return request<Trip>(`/api/trips/${tripId}/phase`, { method: "PUT", data: { phase } })
  },
  duplicateTrip: (tripId: number, data: { name?: string; startDate: string }) => {
    return request<any>(`/api/trips/${tripId}/duplicate`, { method: "POST", data })
  },
  deleteTrip: (tripId: number) => {
    return request<void>(`/api/trips/${tripId}`, { method: "DELETE" })
  },
}
