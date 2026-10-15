import { itineraryApi } from "@/client/api/itinerary-api"
import { tripApi } from "@/client/api/trip-api"
import { getErrorMessage } from "@/client/errors"
import type { ItineraryDay, TripWeather } from "@/client/types"
import { type MapPoint, TripMap } from "@/components/common/TripMap"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ConfirmDialog } from "@/components/ui/confirm-dialog"
import { formatCurrency } from "@/lib/utils"
import { useMutation } from "@tanstack/react-query"
import { Calendar, CloudRain, CloudSun, Download, Map as MapIcon, Plus, Sparkles, Utensils } from "lucide-react"
import { useMemo, useState } from "react"
import { toast } from "sonner"
import { ActivityCard } from "./ActivityCard"
import { AddActivityModal } from "./AddActivityModal"
import { ExplanationModal } from "./ExplanationModal"

interface DayTimelineProps {
  tripId: number
  currency: string
  days: ItineraryDay[]
  weather?: TripWeather
  onRefresh: () => void
  onOpenAiChat: () => void
}

export function DayTimeline({ tripId, currency, days, weather, onRefresh, onOpenAiChat }: DayTimelineProps) {
  const [selectedDayNumber, setSelectedDayNumber] = useState(1)
  const [explainingActivityId, setExplainingActivityId] = useState<number | null>(null)
  const [isAddModalOpen, setIsAddModalOpen] = useState(false)
  const [deletingActivityId, setDeletingActivityId] = useState<number | null>(null)
  const [showMap, setShowMap] = useState(true)
  const [selectedPointId, setSelectedPointId] = useState<number | string | null>(null)

  const activeDay = days.find((d) => d.dayNumber === selectedDayNumber) || days[0]
  const activeDayNumber = activeDay?.dayNumber ?? selectedDayNumber
  const dayWeather = weather?.days?.find((w) => w.dayNumber === activeDayNumber)

  const mapPoints = useMemo(() => {
    if (!activeDay) return []
    return activeDay.activities
      .map((a, idx) => ({
        id: a.id,
        name: a.place?.name || a.title,
        latitude: a.place?.latitude ?? Number.NaN,
        longitude: a.place?.longitude ?? Number.NaN,
        address: a.place?.address || undefined,
        category: a.place?.category || undefined,
        time:
          a.startTime && a.endTime ? `${a.startTime.slice(0, 5)} - ${a.endTime.slice(0, 5)}` : a.startTime?.slice(0, 5),
        orderIndex: idx,
        rating: a.place?.rating,
      }))
      .filter((p) => !Number.isNaN(p.latitude) && !Number.isNaN(p.longitude))
  }, [activeDay])

  const totalEstimatedCost = days.reduce(
    (sum, d) => sum + d.activities.reduce((s, a) => s + (a.estimatedCost || 0), 0),
    0,
  )

  const exportMutation = useMutation({
    mutationFn: () => tripApi.exportIcs(tripId),
    onSuccess: () => toast.success("Đã tải file lịch trình .ics"),
    onError: (err) => toast.error(getErrorMessage(err, "Không thể xuất file .ics")),
  })

  const deleteMutation = useMutation({
    mutationFn: (activityId: number) => itineraryApi.deleteActivity(activityId),
    onSuccess: () => {
      toast.success("Đã xoá hoạt động")
      setDeletingActivityId(null)
      onRefresh()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể xoá hoạt động")),
  })

  const handleDeleteActivity = (id: number) => {
    setDeletingActivityId(id)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar">
          {days.map((day) => (
            <button
              type="button"
              key={day.id}
              aria-pressed={activeDayNumber === day.dayNumber}
              onClick={() => setSelectedDayNumber(day.dayNumber)}
              className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                activeDayNumber === day.dayNumber
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
          <Button
            size="sm"
            variant={showMap ? "default" : "outline"}
            onClick={() => setShowMap(!showMap)}
            className="text-xs gap-1.5"
          >
            <MapIcon className="h-3.5 w-3.5" /> {showMap ? "Ẩn bản đồ" : "Bản đồ lộ trình"}
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => exportMutation.mutate()}
            disabled={exportMutation.isPending}
            className="text-xs gap-1.5"
          >
            <Download className="h-3.5 w-3.5" /> {exportMutation.isPending ? "Đang xuất..." : "Xuất file .ics"}
          </Button>
          <Button size="sm" onClick={() => setIsAddModalOpen(true)} className="text-xs gap-1.5">
            <Plus className="h-3.5 w-3.5" /> Thêm điểm đến
          </Button>
        </div>
      </div>

      <div className="p-3.5 rounded-2xl bg-gradient-to-r from-blue-500/10 via-indigo-500/10 to-violet-500/10 border border-blue-500/20 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-blue-500/20 text-blue-600 dark:text-blue-400 shrink-0">
            <Utensils className="h-4 w-4" aria-hidden />
          </div>
          <div>
            <div className="font-semibold text-foreground flex items-center gap-2">
              <span>Đa phương án & Cân đối ngân sách</span>
              <Badge
                variant="secondary"
                className="text-[10px] bg-blue-500/20 text-blue-600 dark:text-blue-400 border-none font-bold"
              >
                Mới
              </Badge>
            </div>
            <p className="text-muted-foreground mt-0.5">
              Các slot Ăn uống & Lưu trú có sẵn 3 lựa chọn (Tiết kiệm, Tiêu chuẩn, Cao cấp). Bấm chọn trực tiếp để tự
              động cập nhật ngân sách.
            </p>
          </div>
        </div>
        <div className="shrink-0 flex items-center gap-2 bg-background/80 px-3 py-1.5 rounded-xl border border-border/60">
          <span className="text-muted-foreground font-medium">Ước tính lịch trình:</span>
          <span className="font-extrabold text-blue-600 dark:text-blue-400">
            {formatCurrency(totalEstimatedCost, currency)}
          </span>
        </div>
      </div>

      {dayWeather && (
        <div className="p-3.5 rounded-xl bg-gradient-to-r from-sky-500/10 via-blue-500/10 to-indigo-500/10 border border-sky-500/20 flex items-center justify-between text-xs">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-sky-500/20 text-sky-600 dark:text-sky-400">
              {(dayWeather.precipitationProbability ?? 0) > 40 ? (
                <CloudRain className="h-5 w-5" aria-hidden />
              ) : (
                <CloudSun className="h-5 w-5" aria-hidden />
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
                Nhiệt độ: {dayWeather.tempMin ?? "--"}°C – {dayWeather.tempMax ?? "--"}°C &bull; Khả năng mưa:{" "}
                {dayWeather.precipitationProbability ?? "--"}%
              </p>
            </div>
          </div>
        </div>
      )}

      {showMap && mapPoints.length > 0 && (
        <div className="rounded-2xl overflow-hidden border border-border shadow-sm">
          <div className="bg-muted/40 px-4 py-2 border-b border-border flex items-center justify-between text-xs">
            <span className="font-semibold flex items-center gap-1.5">
              <MapIcon className="h-3.5 w-3.5 text-primary" /> Lộ trình di chuyển Ngày {activeDayNumber} (
              {mapPoints.length} điểm đến)
            </span>
            <span className="text-muted-foreground text-[11px] hidden sm:inline">
              Bấm vào điểm để xem thông tin & chỉ đường
            </span>
          </div>
          <TripMap
            points={mapPoints}
            height="h-64 sm:h-72"
            selectedPointId={selectedPointId}
            onSelectPoint={(p: MapPoint) => setSelectedPointId(p.id)}
            showRoute={true}
          />
        </div>
      )}

      {!activeDay || activeDay.activities.length === 0 ? (
        <div className="py-12 text-center rounded-2xl border border-dashed border-border p-8 bg-muted/20">
          <Calendar className="h-10 w-10 text-muted-foreground mx-auto mb-3 opacity-60" aria-hidden />
          <h4 className="font-bold text-base">Chưa có hoạt động nào trong Ngày {activeDayNumber}</h4>
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
        <ol className="relative pl-6 space-y-4 before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-border">
          {activeDay.activities.map((activity) => (
            <li key={activity.id} className="relative group">
              <div className="absolute -left-6 top-4 h-3.5 w-3.5 rounded-full border-2 border-primary bg-background flex items-center justify-center">
                <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              </div>
              <ActivityCard
                activity={activity}
                currency={currency}
                deleting={deleteMutation.isPending && deleteMutation.variables === activity.id}
                onExplain={setExplainingActivityId}
                onDelete={handleDeleteActivity}
                onChanged={onRefresh}
              />
            </li>
          ))}
        </ol>
      )}

      <ExplanationModal activityId={explainingActivityId} onClose={() => setExplainingActivityId(null)} />

      <AddActivityModal
        tripId={tripId}
        dayNumber={activeDayNumber}
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={onRefresh}
      />

      <ConfirmDialog
        open={deletingActivityId !== null}
        onOpenChange={(open) => !open && setDeletingActivityId(null)}
        title="Xoá hoạt động"
        description="Bạn có chắc chắn muốn xoá hoạt động này khỏi lịch trình không?"
        confirmText="Xoá hoạt động"
        variant="destructive"
        isLoading={deleteMutation.isPending}
        onConfirm={() => {
          if (deletingActivityId) deleteMutation.mutate(deletingActivityId)
        }}
      />
    </div>
  )
}
