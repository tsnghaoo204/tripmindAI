import { request } from "../client"
import type { Place, PlaceRating, PlaceVerdict, SavedPlace } from "../types"

export type { Place }

export const placeApi = {
  search: (query: string, destination?: string) => {
    const q = new URLSearchParams({ query })
    if (destination) q.set("destination", destination)
    return request<Place[]>(`/api/places/search?${q.toString()}`)
  },

  getSavedPlaces: () => request<SavedPlace[]>("/api/me/saved-places"),

  savePlace: (placeId: number) => request<SavedPlace>(`/api/places/${placeId}/save`, { method: "POST" }),

  saveByExternalId: (data: { provider: string; externalId: string }) =>
    request<SavedPlace>("/api/me/saved-places", { method: "POST", data }),

  unsavePlace: (placeId: number) => request<void>(`/api/places/${placeId}/save`, { method: "DELETE" }),

  getTripReviewPlaces: (tripId: number) => request<PlaceRating[]>(`/api/trips/${tripId}/review/places`),

  ratePlace: (tripId: number, placeId: number, verdict: PlaceVerdict) =>
    request<PlaceRating>(`/api/trips/${tripId}/places/${placeId}/rating`, { method: "PUT", data: { verdict } }),
}
