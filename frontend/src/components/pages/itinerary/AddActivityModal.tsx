import { itineraryApi } from "@/client/api/itinerary-api"
import { placeApi } from "@/client/api/place-api"
import { getErrorMessage } from "@/client/errors"
import type { ActivityType, Place } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { formatCurrency } from "@/lib/utils"
import { useQuery } from "@tanstack/react-query"
import { Bookmark, Clock, DollarSign, Heart, MapPin, Plus, Search, Sparkles, Star } from "lucide-react"
import { useMemo, useState } from "react"
import { toast } from "sonner"

interface AddActivityModalProps {
  tripId: number
  dayNumber: number
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

const ACTIVITY_TYPES: Array<{ id: ActivityType; label: string }> = [
  { id: "SIGHTSEEING", label: "Tham quan / Thắng cảnh" },
  { id: "FOOD", label: "Ăn uống / Nhà hàng" },
  { id: "ACCOMMODATION", label: "Nghỉ ngơi / Khách sạn" },
  { id: "TRANSPORT", label: "Di chuyển" },
  { id: "REST", label: "Nghỉ ngơi tự do" },
  { id: "OTHER", label: "Khác" },
]

function inferActivityType(place: Place): ActivityType {
  const cat = (place.category || place.primaryType || "").toUpperCase()
  if (cat.includes("FOOD") || cat.includes("RESTAURANT") || cat.includes("CAFE") || cat.includes("COFFEE")) {
    return "FOOD"
  }
  if (cat.includes("HOTEL") || cat.includes("ACCOMMODATION") || cat.includes("LODGING") || cat.includes("RESORT")) {
    return "ACCOMMODATION"
  }
  return "SIGHTSEEING"
}

export function AddActivityModal({ tripId, dayNumber, isOpen, onClose, onSuccess }: AddActivityModalProps) {
  const [activeTab, setActiveTab] = useState<string>("manual")
  const [title, setTitle] = useState("")
  const [activityType, setActivityType] = useState<ActivityType>("SIGHTSEEING")
  const [startTime, setStartTime] = useState("09:00")
  const [endTime, setEndTime] = useState("11:00")
  const [estimatedCost, setEstimatedCost] = useState("")
  const [notes, setNotes] = useState("")
  const [loading, setLoading] = useState(false)
  const [costHint, setCostHint] = useState<string | null>(null)
  const [selectedPlace, setSelectedPlace] = useState<Place | null>(null)
  const [savedSearch, setSavedSearch] = useState("")

  const { data: savedPlaces = [], isLoading: isSavedLoading } = useQuery({
    queryKey: ["saved-places"],
    queryFn: placeApi.getSavedPlaces,
    enabled: isOpen,
  })

  const filteredSavedPlaces = useMemo(() => {
    const q = savedSearch.trim().toLowerCase()
    if (!q) return savedPlaces
    return savedPlaces.filter(
      (sp) =>
        sp.place?.name?.toLowerCase().includes(q) ||
        sp.place?.address?.toLowerCase().includes(q) ||
        sp.place?.category?.toLowerCase().includes(q),
    )
  }, [savedPlaces, savedSearch])

  const handleSelectSavedPlace = (place: Place) => {
    setTitle(place.name)
    setSelectedPlace(place)
    setActivityType(inferActivityType(place))
    setActiveTab("manual")
    toast.info(`Đã chọn "${place.name}". Bạn có thể chỉnh lại giờ và bấm lưu!`)
  }

  const handleQuickAdd = async (place: Place, start: string, end: string) => {
    setLoading(true)
    try {
      await itineraryApi.addActivity(tripId, {
        dayNumber,
        title: place.name,
        activityType: inferActivityType(place),
        placeId: place.id,
        placeExternalId: place.externalId,
        startTime: start,
        endTime: end,
        notes: place.address ? `Địa chỉ: ${place.address}` : undefined,
      })
      toast.success(`Đã thêm "${place.name}" vào ngày ${dayNumber}!`)
      onSuccess()
      onClose()
    } catch (err) {
      toast.error(getErrorMessage(err, "Không thể thêm hoạt động"))
    } finally {
      setLoading(false)
    }
  }

  const handleFetchCostHint = async () => {
    try {
      const hint = await itineraryApi.getCostEstimate(tripId, {
        placeId: selectedPlace?.id,
        priceLevel: 2,
        activityType,
      })
      if (hint.suggested != null) {
        setEstimatedCost(String(hint.suggested))
        setCostHint(hint.basis || null)
        toast.info(`Gợi ý: ${formatCurrency(hint.suggested, hint.currency || "VND")}`)
      } else if (hint.reason) {
        toast.warning(`Không có dữ liệu ước tính: ${hint.reason}`)
      }
    } catch (err) {
      toast.error(getErrorMessage(err, "Không tìm thấy ước tính giá cho mục này"))
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await itineraryApi.addActivity(tripId, {
        dayNumber,
        title,
        activityType,
        placeId: selectedPlace?.id,
        placeExternalId: selectedPlace?.externalId,
        startTime: startTime || undefined,
        endTime: endTime || undefined,
        estimatedCost: estimatedCost ? Number(estimatedCost) : undefined,
        notes: notes || undefined,
      })
      toast.success("Đã thêm hoạt động vào lịch trình!")
      setTitle("")
      setNotes("")
      setEstimatedCost("")
      setCostHint(null)
      setSelectedPlace(null)
      onSuccess()
      onClose()
    } catch (err) {
      toast.error(getErrorMessage(err, "Lỗi khi thêm hoạt động"))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <div className="p-2 rounded-lg bg-primary/10 text-primary">
            <Plus className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle>Thêm Hoạt Động Ngày {dayNumber}</DialogTitle>
            <DialogDescription>Lên lịch chi tiết các điểm đến trong hành trình</DialogDescription>
          </div>
        </DialogHeader>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <div className="px-5 pt-3 border-b">
            <TabsList className="w-full grid grid-cols-2">
              <TabsTrigger value="manual" className="text-xs gap-1.5">
                <Plus className="h-3.5 w-3.5" /> Nhập thủ công
              </TabsTrigger>
              <TabsTrigger value="saved" className="text-xs gap-1.5">
                <Bookmark className="h-3.5 w-3.5" /> Địa điểm đã lưu ({savedPlaces.length})
              </TabsTrigger>
            </TabsList>
          </div>

          <TabsContent value="manual" className="mt-0">
            <form onSubmit={handleSubmit} className="flex flex-col min-h-0">
              <DialogBody className="space-y-4 text-xs">
                {selectedPlace && (
                  <div className="p-2.5 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <MapPin className="h-4 w-4 text-primary shrink-0" />
                      <div>
                        <span className="font-semibold text-foreground">Đã gắn: {selectedPlace.name}</span>
                        {selectedPlace.address && (
                          <p className="text-[11px] text-muted-foreground truncate max-w-[260px]">
                            {selectedPlace.address}
                          </p>
                        )}
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelectedPlace(null)}
                      className="h-6 text-[11px] text-muted-foreground hover:text-foreground"
                    >
                      Bỏ gắn
                    </Button>
                  </div>
                )}

                <div className="space-y-1.5">
                  <label htmlFor="activity-title" className="text-xs font-medium">
                    Tên hoạt động / Địa điểm *
                  </label>
                  <Input
                    id="activity-title"
                    required
                    placeholder="VD: Cà phê sáng ngắm biển Mỹ Khê"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                  />
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="activity-type" className="text-xs font-medium">
                    Phân loại
                  </label>
                  <select
                    id="activity-type"
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs focus:ring-2 focus:ring-ring outline-none"
                    value={activityType}
                    onChange={(e) => setActivityType(e.target.value as ActivityType)}
                  >
                    {ACTIVITY_TYPES.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <label htmlFor="activity-start" className="text-xs font-medium flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5 text-primary" aria-hidden /> Bắt đầu
                    </label>
                    <Input
                      id="activity-start"
                      type="time"
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label htmlFor="activity-end" className="text-xs font-medium flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5 text-primary" aria-hidden /> Kết thúc
                    </label>
                    <Input id="activity-end" type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between items-center">
                    <label htmlFor="activity-cost" className="text-xs font-medium flex items-center gap-1">
                      <DollarSign className="h-3.5 w-3.5 text-emerald-500" aria-hidden /> Chi phí ước tính (VND)
                    </label>
                    <button
                      type="button"
                      onClick={handleFetchCostHint}
                      className="text-[11px] text-primary hover:underline flex items-center gap-1 cursor-pointer"
                    >
                      <Sparkles className="h-3 w-3" aria-hidden /> Gợi ý giá
                    </button>
                  </div>
                  <Input
                    id="activity-cost"
                    type="number"
                    min={0}
                    placeholder="VD: 150000"
                    value={estimatedCost}
                    onChange={(e) => setEstimatedCost(e.target.value)}
                  />
                  {costHint && <p className="text-[10px] text-muted-foreground">{costHint}</p>}
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="activity-notes" className="text-xs font-medium">
                    Ghi chú
                  </label>
                  <Input
                    id="activity-notes"
                    placeholder="VD: Nhớ mang máy ảnh, đặt bàn trước..."
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                  />
                </div>
              </DialogBody>

              <DialogFooter>
                <Button type="button" variant="outline" size="sm" onClick={onClose}>
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={loading}>
                  {loading ? "Đang lưu..." : "Thêm vào ngày"}
                </Button>
              </DialogFooter>
            </form>
          </TabsContent>

          <TabsContent value="saved" className="mt-0">
            <DialogBody className="space-y-3 text-xs">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                <Input
                  placeholder="Tìm trong danh sách địa điểm đã lưu..."
                  value={savedSearch}
                  onChange={(e) => setSavedSearch(e.target.value)}
                  className="pl-8 h-8 text-xs"
                />
              </div>

              {isSavedLoading ? (
                <div className="py-12 text-center text-muted-foreground">Đang tải địa điểm đã lưu...</div>
              ) : filteredSavedPlaces.length === 0 ? (
                <div className="py-12 text-center space-y-2">
                  <Heart className="h-8 w-8 text-rose-400 mx-auto opacity-40" />
                  <p className="font-semibold text-foreground">Không có địa điểm nào</p>
                  <p className="text-muted-foreground max-w-xs mx-auto">
                    {savedPlaces.length === 0
                      ? "Bạn chưa lưu địa điểm nào. Vào mục 'Khám phá địa điểm' bấm thả tim để lưu nhé!"
                      : "Không tìm thấy địa điểm khớp từ khóa tìm kiếm."}
                  </p>
                </div>
              ) : (
                <div className="space-y-2 max-h-[340px] overflow-y-auto pr-1">
                  {filteredSavedPlaces.map((sp) => {
                    const place = sp.place
                    return (
                      <div
                        key={sp.id}
                        className="p-3 rounded-xl border border-border/80 hover:border-primary/40 bg-card hover:bg-muted/30 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        <div className="space-y-1 min-w-0 flex-1">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-sm text-foreground truncate">{place.name}</span>
                            {place.rating != null && (
                              <Badge
                                variant="secondary"
                                className="text-[10px] gap-1 px-1.5 text-amber-600 bg-amber-500/10"
                              >
                                <Star className="h-2.5 w-2.5 fill-current" />
                                {place.rating}
                              </Badge>
                            )}
                          </div>
                          {place.address && (
                            <p className="text-[11px] text-muted-foreground truncate">{place.address}</p>
                          )}
                          {place.category && (
                            <Badge variant="outline" className="text-[10px] text-muted-foreground">
                              {place.category}
                            </Badge>
                          )}
                        </div>

                        <div className="flex items-center gap-1.5 shrink-0">
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            onClick={() => handleSelectSavedPlace(place)}
                            className="text-xs h-7.5 gap-1"
                          >
                            Tùy chỉnh
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            onClick={() => handleQuickAdd(place, "09:00", "11:00")}
                            disabled={loading}
                            className="text-xs h-7.5 gap-1 bg-primary text-primary-foreground font-semibold"
                          >
                            <Plus className="h-3 w-3" /> Thêm nhanh
                          </Button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </DialogBody>
            <DialogFooter>
              <Button type="button" variant="outline" size="sm" onClick={onClose}>
                Đóng
              </Button>
            </DialogFooter>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  )
}
