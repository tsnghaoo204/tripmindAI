import { request } from "../client"
import type { TripWeather } from "../types"

export const weatherApi = {
  getTripWeather: async (tripId: number): Promise<TripWeather> => {
    return request<TripWeather>(`/api/trips/${tripId}/weather`)
  },
}
