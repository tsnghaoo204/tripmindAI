import { request } from "../client"

export interface Place {
  id: number
  provider: string
  externalId: string
  name: string
  address: string
  latitude: number
  longitude: number
  rating?: number
  userRatingsTotal?: number
  priceLevel?: number
  category?: string
  primaryType?: string
  primaryTypeDisplayName?: string
  types?: string[]
  photos?: string[]
}

export const placeApi = {
  search: async (query: string, destinationId?: number): Promise<Place[]> => {
    const q = new URLSearchParams({ query })
    if (destinationId) q.set("destinationId", String(destinationId))
    return request<Place[]>(`/api/places/search?${q.toString()}`)
  },

  getSavedPlaces: async (): Promise<Place[]> => {
    return request<Place[]>("/api/me/saved-places")
  },

  savePlace: async (placeId: number): Promise<void> => {
    return request<void>(`/api/places/${placeId}/save`, { method: "POST" })
  },

  saveByExternalId: async (data: { provider: string; externalId: string }): Promise<void> => {
    return request<void>("/api/me/saved-places", { method: "POST", data })
  },

  unsavePlace: async (placeId: number): Promise<void> => {
    return request<void>(`/api/places/${placeId}/save`, { method: "DELETE" })
  },

  getTripReviewPlaces: async (tripId: number): Promise<Array<Place & { verdict?: "LIKE" | "DISLIKE" }>> => {
    return request<Array<Place & { verdict?: "LIKE" | "DISLIKE" }>>(`/api/trips/${tripId}/review/places`)
  },

  ratePlace: async (tripId: number, placeId: number, verdict: "LIKE" | "DISLIKE"): Promise<void> => {
    return request<void>(`/api/trips/${tripId}/places/${placeId}/rating`, {
      method: "PUT",
      data: { verdict },
    })
  },
}
