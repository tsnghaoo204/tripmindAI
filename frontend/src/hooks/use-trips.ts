import { itineraryApi } from "@/client/api/itinerary-api"
import { tripApi } from "@/client/api/trip-api"
import { weatherApi } from "@/client/api/weather-api"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useCallback } from "react"
import { queryKeys } from "./query-keys"

export function useTrips(status?: "upcoming" | "past") {
  return useQuery({
    queryKey: queryKeys.trips(status),
    queryFn: () => tripApi.getTrips(status),
  })
}

export function useTrip(tripId: number) {
  return useQuery({
    queryKey: queryKeys.tripDetail(tripId),
    queryFn: () => tripApi.getTripById(tripId),
    enabled: Number.isFinite(tripId),
  })
}

export function useItinerary(tripId: number) {
  return useQuery({
    queryKey: queryKeys.itinerary(tripId),
    queryFn: () => itineraryApi.getItinerary(tripId),
    enabled: Number.isFinite(tripId),
  })
}

export function useTripWeather(tripId: number) {
  return useQuery({
    queryKey: queryKeys.weather(tripId),
    queryFn: () => weatherApi.getTripWeather(tripId),
    enabled: Number.isFinite(tripId),
    retry: false,
  })
}

/** Returns a callback that refetches every query belonging to the trip. */
export function useInvalidateTrip(tripId: number) {
  const queryClient = useQueryClient()
  return useCallback(() => {
    queryClient.invalidateQueries({ queryKey: queryKeys.trip(tripId) })
    queryClient.invalidateQueries({ queryKey: ["trips"] })
  }, [queryClient, tripId])
}
