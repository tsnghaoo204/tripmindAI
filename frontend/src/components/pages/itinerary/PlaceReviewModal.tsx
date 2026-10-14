import { placeApi } from "@/client/api/place-api"
import { getErrorMessage } from "@/client/errors"
import type { PlaceRating, PlaceVerdict } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { queryKeys } from "@/hooks/query-keys"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { MapPin, Sparkles, ThumbsDown, ThumbsUp } from "lucide-react"
import { toast } from "sonner"

interface PlaceReviewModalProps {
  tripId: number
  isOpen: boolean
  onClose: () => void
}

export function PlaceReviewModal({ tripId, isOpen, onClose }: PlaceReviewModalProps) {
  const queryClient = useQueryClient()
  const { data: ratings = [], isLoading } = useQuery({
    queryKey: queryKeys.reviewPlaces(tripId),
    queryFn: () => placeApi.getTripReviewPlaces(tripId),
    enabled: isOpen,
  })

  const rateMutation = useMutation({
    mutationFn: ({ placeId, verdict }: { placeId: number; verdict: PlaceVerdict }) =>
      placeApi.ratePlace(tripId, placeId, verdict),
    onSuccess: (_res, { placeId, verdict }) => {
      toast.success(verdict === "LIKE" ? "Đã thích địa điểm!" : "Đã không thích địa điểm!")
      queryClient.setQueryData<PlaceRating[]>(queryKeys.reviewPlaces(tripId), (prev) =>
        prev?.map((r) => (r.place.id === placeId ? { ...r, verdict } : r)),
      )
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi đánh giá địa điểm")),
  })

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <div className="p-2 rounded-lg bg-primary/10 text-primary">
            <Sparkles className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle>Đánh Giá Địa Điểm Sau Chuyến Đi</DialogTitle>
            <DialogDescription>AI sẽ ưu tiên hoặc loại bỏ các địa điểm này trong tương lai</DialogDescription>
          </div>
        </DialogHeader>

        <DialogBody className="space-y-4">
          {isLoading ? (
            <div className="py-8 text-center text-xs text-muted-foreground">Đang tải địa điểm trong lịch trình...</div>
          ) : ratings.length === 0 ? (
            <div className="py-8 text-center text-xs text-muted-foreground">
              Chưa có địa điểm nào được ghé thăm trong chuyến đi này.
            </div>
          ) : (
            ratings.map(({ place, verdict }) => (
              <div
                key={place.id}
                className="p-3.5 rounded-xl border border-border bg-card/60 flex items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="font-semibold text-sm flex items-center gap-2">
                    <span>{place.name}</span>
                    {verdict && (
                      <Badge
                        variant="outline"
                        className={`text-[10px] ${
                          verdict === "LIKE"
                            ? "bg-emerald-500/10 text-emerald-600 border-emerald-500/20"
                            : "bg-rose-500/10 text-rose-600 border-rose-500/20"
                        }`}
                      >
                        {verdict === "LIKE" ? "Đã thích" : "Đã không thích"}
                      </Badge>
                    )}
                  </div>
                  {place.address && (
                    <div className="text-xs text-muted-foreground flex items-center gap-1">
                      <MapPin className="h-3.5 w-3.5 shrink-0" aria-hidden />
                      <span className="line-clamp-1">{place.address}</span>
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <Button
                    size="sm"
                    aria-pressed={verdict === "LIKE"}
                    variant={verdict === "LIKE" ? "default" : "outline"}
                    disabled={rateMutation.isPending}
                    className={`h-8 text-xs gap-1 ${verdict === "LIKE" ? "bg-emerald-600 hover:bg-emerald-700 text-white" : ""}`}
                    onClick={() => rateMutation.mutate({ placeId: place.id, verdict: "LIKE" })}
                  >
                    <ThumbsUp className="h-3.5 w-3.5" /> Thích
                  </Button>
                  <Button
                    size="sm"
                    aria-pressed={verdict === "DISLIKE"}
                    variant={verdict === "DISLIKE" ? "destructive" : "outline"}
                    disabled={rateMutation.isPending}
                    className="h-8 text-xs gap-1"
                    onClick={() => rateMutation.mutate({ placeId: place.id, verdict: "DISLIKE" })}
                  >
                    <ThumbsDown className="h-3.5 w-3.5" /> Không thích
                  </Button>
                </div>
              </div>
            ))
          )}
        </DialogBody>
      </DialogContent>
    </Dialog>
  )
}
