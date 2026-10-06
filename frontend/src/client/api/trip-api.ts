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
  deleteTrip: (tripId: number) => {
    return request<void>(`/api/trips/${tripId}`, { method: "DELETE" })
  },
}
