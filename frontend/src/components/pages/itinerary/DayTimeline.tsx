import React, { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  Clock,
  MapPin,
  Sparkles,
  Plus,
  Trash2,
  Calendar,
  CloudSun,
  CloudRain,
  Sun,
  Download,
  HelpCircle,
  CheckCircle,
  ArrowUpDown,
  Utensils,
  Hotel,
  Car,
} from "lucide-react"
import type { ItineraryDay, Activity, TripWeather } from "@/client/types"
import { formatCurrency } from "@/lib/utils"
import { ExplanationModal } from "./ExplanationModal"
import { AddActivityModal } from "./AddActivityModal"
import { toast } from "sonner"
import { itineraryApi } from "@/client/api/itinerary-api"

interface DayTimelineProps {
  tripId: number
  days: ItineraryDay[]
  weather?: TripWeather
  onRefresh: () => void
  onOpenAiChat: () => void
}

export function DayTimeline({
  tripId,
  days,
  weather,
  onRefresh,
  onOpenAiChat,
}: DayTimelineProps) {
  const [selectedDayNumber, setSelectedDayNumber] = useState(1)
  const [explainingActivityId, setExplainingActivityId] = useState<number | null>(null)
  const [isAddModalOpen, setIsAddModalOpen] = useState(false)

  const activeDay = days.find((d) => d.dayNumber === selectedDayNumber) || days[0]
  const dayWeather = weather?.days?.find((w) => w.dayNumber === selectedDayNumber)

  const handleExportIcs = () => {
    window.open(`/api/trips/${tripId}/export.ics`, "_blank")
    toast.success("Đang tải file lịch trình .ics...")
  }

  const handleDeleteActivity = async (id: number) => {
    if (!confirm("Bạn có chắc muốn xoá hoạt động này khỏi lịch trình?")) return
    try {
      await itineraryApi.deleteActivity(id)
      toast.success("Đã xoá hoạt động")
      onRefresh()
    } catch {
      toast.error("Không thể xoá hoạt động")
    }
  }

  const getActivityIcon = (type: string) => {
    switch (type) {
      case "FOOD":
        return <Utensils className="h-4 w-4 text-amber-500" />
      case "ACCOMMODATION":
        return <Hotel className="h-4 w-4 text-indigo-500" />
      case "TRANSPORT":
        return <Car className="h-4 w-4 text-blue-500" />
      default:
        return <MapPin className="h-4 w-4 text-emerald-500" />
    }
  }

  const getPlaceTagBadge = (place?: {
    category?: string
    primaryType?: string
    primaryTypeDisplayName?: string
    types?: string[]
  }) => {
    if (!place) return null
    const rawType = (place.primaryType || place.category || "").toLowerCase()
    const types = (place.types || []).map((t) => t.toLowerCase())
    const displayName = place.primaryTypeDisplayName

    // 1. Quán cà phê / Quán nước / Đồ uống
    if (
      rawType === "cafe" ||
      rawType === "coffee_shop" ||
      rawType === "tea_house" ||
      rawType === "bar" ||
      rawType === "pub" ||
      rawType === "wine_bar" ||
      types.includes("cafe") ||
      types.includes("coffee_shop") ||
      types.includes("tea_house") ||
      types.includes("bar") ||
      types.includes("pub")
    ) {
      return {
        label: displayName || "Quán cà phê / Đồ uống",
        icon: "☕",
        color: "bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/25",
      }
    }

    // 2. Nhà hàng / Quán ăn
    if (
      rawType.includes("restaurant") ||
      types.some((t) => t.includes("restaurant")) ||
      types.includes("food_court") ||
      types.includes("diner") ||
      types.includes("meal_takeaway")
    ) {
      return {
        label: displayName || "Nhà hàng / Quán ăn",
        icon: "🍽️",
        color: "bg-orange-500/10 text-orange-700 dark:text-orange-400 border-orange-500/25",
      }
    }

    // 3. Khách sạn / Lưu trú
    if (
      rawType.includes("hotel") ||
      rawType.includes("lodging") ||
      rawType.includes("resort") ||
      types.includes("lodging") ||
      types.includes("hotel") ||
      types.includes("resort_hotel") ||
      types.includes("guest_house") ||
      types.includes("hostel")
    ) {
      return {
        label: displayName || "Khách sạn / Lưu trú",
        icon: "🏨",
        color: "bg-blue-500/10 text-blue-700 dark:text-blue-400 border-blue-500/25",
      }
    }

    // 4. Điểm tham quan / Du lịch
    if (
      rawType.includes("attraction") ||
      rawType.includes("museum") ||
      rawType.includes("park") ||
      types.includes("tourist_attraction") ||
      types.includes("historical_landmark") ||
      types.includes("museum") ||
      types.includes("park") ||
      types.includes("natural_feature")
    ) {
      return {
        label: displayName || "Điểm tham quan",
        icon: "📸",
        color: "bg-purple-500/10 text-purple-700 dark:text-purple-400 border-purple-500/25",
      }
    }

    if (displayName) {
      return {
        label: displayName,
        icon: "🏷️",
        color: "bg-slate-500/10 text-slate-700 dark:text-slate-400 border-slate-500/25",
      }
    }

    if (place.category && place.category !== "GENERAL") {
      return {
        label: place.category,
        icon: "🏷️",
        color: "bg-slate-500/10 text-slate-700 dark:text-slate-400 border-slate-500/25",
      }
    }

    return null
  }

  return (
    <div className="space-y-6">
      {/* Day Selector Tabs & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar">
          {days.map((day) => (
            <button
              key={day.id}
              onClick={() => setSelectedDayNumber(day.dayNumber)}
              className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                selectedDayNumber === day.dayNumber
                  ? "bg-primary text-primary-foreground shadow-md shadow-primary/20 scale-105"
                  : "bg-muted/70 text-muted-foreground hover:bg-accent hover:text-foreground"
              }`}
            >
              Ngày {day.dayNumber}
              <span className="block text-[10px] opacity-80 font-normal">
                {new Date(day.date).toLocaleDateString("vi-VN", { month: "numeric", day: "numeric" })}
              </span>
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <Button size="sm" variant="outline" onClick={handleExportIcs} className="text-xs gap-1.5">
            <Download className="h-3.5 w-3.5" /> Xuất file .ics
          </Button>
          <Button size="sm" onClick={() => setIsAddModalOpen(true)} className="text-xs gap-1.5">
            <Plus className="h-3.5 w-3.5" /> Thêm điểm đến
          </Button>
        </div>
      </div>

      {/* Day Weather Banner */}
      {dayWeather && (
        <div className="p-3.5 rounded-xl bg-gradient-to-r from-sky-500/10 via-blue-500/10 to-indigo-500/10 border border-sky-500/20 flex items-center justify-between text-xs">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-sky-500/20 text-sky-600 dark:text-sky-400">
              {dayWeather.precipitationProbability > 40 ? (
                <CloudRain className="h-5 w-5" />
              ) : (
                <CloudSun className="h-5 w-5" />
              )}
            </div>
            <div>
              <div className="font-semibold text-foreground flex items-center gap-2">
                <span>{dayWeather.summary}</span>
                <Badge variant="outline" className="text-[10px] h-4">
                  {dayWeather.source === "FORECAST" ? "Dự báo thời gian thực" : "Khí hậu cùng kỳ"}
                </Badge>
              </div>
              <p className="text-muted-foreground mt-0.5">
                Nhiệt độ: {dayWeather.tempMin}°C – {dayWeather.tempMax}°C &bull; Khả năng mưa: {dayWeather.precipitationProbability}%
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Timeline Activities */}
      {!activeDay || activeDay.activities.length === 0 ? (
        <div className="py-12 text-center rounded-2xl border border-dashed border-border p-8 bg-muted/20">
          <Calendar className="h-10 w-10 text-muted-foreground mx-auto mb-3 opacity-60" />
          <h4 className="font-bold text-base">Chưa có hoạt động nào trong Ngày {selectedDayNumber}</h4>
          <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-1 mb-4">
            Bạn có thể tự thêm địa điểm hoặc nhờ Trợ lý AI tự động gợi ý lịch trình thông minh.
          </p>
          <div className="flex justify-center gap-3">
            <Button size="sm" onClick={() => setIsAddModalOpen(true)} variant="outline">
              Tự thêm hoạt động
            </Button>
            <Button size="sm" onClick={onOpenAiChat} className="gap-1.5">
              <Sparkles className="h-3.5 w-3.5" /> Nhờ AI gợi ý ngay
            </Button>
          </div>
        </div>
      ) : (
        <div className="relative pl-6 space-y-4 before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-border">
          {activeDay.activities.map((activity, idx) => (
            <div key={activity.id} className="relative group">
              {/* Timeline marker */}
              <div className="absolute -left-6 top-4 h-3.5 w-3.5 rounded-full border-2 border-primary bg-background flex items-center justify-center">
                <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              </div>

              <Card className="hover:border-primary/40 transition-all hover:shadow-sm">
                <CardContent className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <div className="p-2 rounded-xl bg-muted/80 text-foreground shrink-0 mt-0.5">
                      {getActivityIcon(activity.activityType)}
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-bold text-sm text-foreground">{activity.title}</span>
                        {(activity.place?.name || activity.placeName) && (
                          <Badge variant="secondary" className="text-xs bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20 flex items-center gap-1 font-semibold">
                            <MapPin className="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
                            {activity.place?.name || activity.placeName}
                          </Badge>
                        )}
                        {activity.place && (() => {
                          const tag = getPlaceTagBadge(activity.place)
                          if (!tag) return null
                          return (
                            <Badge variant="outline" className={`text-[11px] h-5 px-1.5 flex items-center gap-1 font-medium ${tag.color}`}>
                              <span>{tag.icon}</span>
                              <span>{tag.label}</span>
                            </Badge>
                          )
                        })()}
                        {activity.fromProposalId && (
                          <Badge variant="outline" className="text-[10px] text-primary border-primary/30 bg-primary/5 flex items-center gap-1">
                            <Sparkles className="h-2.5 w-2.5" /> AI Đề xuất
                          </Badge>
                        )}
                      </div>

                      <div className="flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                        {(activity.startTime || activity.endTime) && (
                          <div className="flex items-center gap-1">
                            <Clock className="h-3 w-3" />
                            <span>
                              {activity.startTime || "--"} - {activity.endTime || "--"}
                            </span>
                          </div>
                        )}
                        {(activity.place?.formattedAddress || activity.place?.address) && (
                          <div className="flex items-center gap-1 text-muted-foreground text-[11px]">
                            <span>📍 {activity.place?.formattedAddress || activity.place?.address}</span>
                          </div>
                        )}
                        {activity.place?.rating && (
                          <div className="flex items-center gap-1 text-amber-500 font-semibold text-[11px]">
                            <span>★ {activity.place.rating}</span>
                            {activity.place.userRatingsTotal && (
                              <span className="text-muted-foreground font-normal">({activity.place.userRatingsTotal})</span>
                            )}
                          </div>
                        )}
                        {activity.estimatedCost != null && (
                          <Badge variant="secondary" className="text-[10px] font-medium flex items-center gap-1">
                            <span>{activity.estimatedCost === 0 ? "Miễn phí" : formatCurrency(activity.estimatedCost)}</span>
                            {activity.estimatedCost > 0 && (activity.estimatedCostSource === "AI" || activity.costSource === "AI") && (
                              <span className="text-[9px] text-primary/80 font-normal" title="Ước tính AI theo giá thị trường địa phương">(Dự toán AI)</span>
                            )}
                            {activity.estimatedCost > 0 && (activity.estimatedCostSource === "PRICE_LEVEL" || activity.costSource === "PRICE_LEVEL") && (
                              <span className="text-[9px] text-muted-foreground font-normal" title="Ước tính từ Google Places">(Google)</span>
                            )}
                          </Badge>
                        )}
                      </div>

                      {activity.notes && (
                        <p className="text-[11px] text-muted-foreground italic mt-1">{activity.notes}</p>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                    {activity.fromProposalId && (
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => setExplainingActivityId(activity.id)}
                        className="h-8 w-8 text-primary hover:bg-primary/10"
                        title="Xem giải trình vì sao AI chọn điểm này"
                      >
                        <HelpCircle className="h-4 w-4" />
                      </Button>
                    )}
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => handleDeleteActivity(activity.id)}
                      className="h-8 w-8 text-destructive hover:bg-destructive/10"
                      title="Xoá hoạt động"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </div>
          ))}
        </div>
      )}

      {/* Explanation Modal */}
      <ExplanationModal
        activityId={explainingActivityId}
        onClose={() => setExplainingActivityId(null)}
      />

      {/* Add Activity Modal */}
      <AddActivityModal
        tripId={tripId}
        dayNumber={selectedDayNumber}
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={onRefresh}
      />
    </div>
  )
}
