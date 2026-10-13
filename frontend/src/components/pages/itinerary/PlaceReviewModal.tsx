import React, { useState, useEffect } from "react"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { X, ThumbsUp, ThumbsDown, MapPin, Sparkles, CheckCircle2 } from "lucide-react"
import { placeApi, type Place } from "@/client/api/place-api"
import { toast } from "sonner"

interface PlaceReviewModalProps {
  tripId: number
  isOpen: boolean
  onClose: () => void
}

export function PlaceReviewModal({ tripId, isOpen, onClose }: PlaceReviewModalProps) {
  const [places, setPlaces] = useState<Array<Place & { verdict?: "LIKE" | "DISLIKE" }>>([])
  const [loading, setLoading] = useState(false)

  const loadPlaces = async () => {
    try {
      setLoading(true)
      const data = await placeApi.getTripReviewPlaces(tripId)
      setPlaces(data)
    } catch {
      toast.error("Không thể tải danh sách địa điểm đánh giá")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (isOpen) {
      loadPlaces()
    }
  }, [isOpen, tripId])

  if (!isOpen) return null

  const handleRate = async (placeId: number, verdict: "LIKE" | "DISLIKE") => {
    try {
      await placeApi.ratePlace(tripId, placeId, verdict)
      toast.success(verdict === "LIKE" ? "Đã thích địa điểm!" : "Đã không thích địa điểm!")
      setPlaces((prev) =>
        prev.map((p) => (p.id === placeId ? { ...p, verdict } : p))
      )
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi đánh giá địa điểm")
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
      <Card className="w-full max-w-lg shadow-2xl border-border animate-in fade-in zoom-in-95">
        <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-base font-bold">Đánh Giá Địa Điểm Sau Chuyến Đi</CardTitle>
              <p className="text-xs text-muted-foreground">
                AI sẽ ưu tiên hoặc loại bỏ các địa điểm này trong tương lai
              </p>
            </div>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} className="rounded-full">
            <X className="h-4 w-4" />
          </Button>
        </CardHeader>

        <CardContent className="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          {loading ? (
            <div className="py-8 text-center text-xs text-muted-foreground">
              Đang tải địa điểm trong lịch trình...
            </div>
          ) : places.length === 0 ? (
            <div className="py-8 text-center text-xs text-muted-foreground">
              Chưa có địa điểm nào được ghé thăm trong chuyến đi này.
            </div>
          ) : (
            places.map((place) => (
              <div
                key={place.id}
                className="p-3.5 rounded-xl border border-border bg-card/60 flex items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="font-semibold text-sm flex items-center gap-2">
                    <span>{place.name}</span>
                    {place.verdict && (
                      <Badge
                        variant="outline"
                        className={`text-[10px] ${
                          place.verdict === "LIKE"
                            ? "bg-emerald-500/10 text-emerald-600 border-emerald-500/20"
                            : "bg-rose-500/10 text-rose-600 border-rose-500/20"
                        }`}
                      >
                        {place.verdict === "LIKE" ? "Đã thích" : "Đã không thích"}
                      </Badge>
                    )}
                  </div>
                  <div className="text-xs text-muted-foreground flex items-center gap-1">
                    <MapPin className="h-3.5 w-3.5 shrink-0" />
                    <span className="line-clamp-1">{place.address}</span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <Button
                    size="sm"
                    variant={place.verdict === "LIKE" ? "default" : "outline"}
                    className={`h-8 text-xs gap-1 ${
                      place.verdict === "LIKE" ? "bg-emerald-600 hover:bg-emerald-700 text-white" : ""
                    }`}
                    onClick={() => handleRate(place.id, "LIKE")}
                  >
                    <ThumbsUp className="h-3.5 w-3.5" /> Thích
                  </Button>
                  <Button
                    size="sm"
                    variant={place.verdict === "DISLIKE" ? "destructive" : "outline"}
                    className="h-8 text-xs gap-1"
                    onClick={() => handleRate(place.id, "DISLIKE")}
                  >
                    <ThumbsDown className="h-3.5 w-3.5" /> Không thích
                  </Button>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  )
}
