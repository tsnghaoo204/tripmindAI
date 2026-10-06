import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { tripApi } from "@/client/api/trip-api"
import type { Trip } from "@/client/types"

export function useTrips(status?: "upcoming" | "past") {
  return useQuery({
    queryKey: ["trips", status],
    queryFn: () => tripApi.getTrips(status),
  })
}

export function useTrip(tripId: number) {
  return useQuery({
    queryKey: ["trip", tripId],
    queryFn: () => tripApi.getTripById(tripId),
    enabled: !!tripId,
  })
}

export function useCreateTrip() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Partial<Trip>) => tripApi.createTrip(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["trips"] })
    },
  })
}
