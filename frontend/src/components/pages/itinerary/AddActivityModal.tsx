import { itineraryApi } from "@/client/api/itinerary-api"
import { getErrorMessage } from "@/client/errors"
import type { ActivityType } from "@/client/types"
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
import { formatCurrency } from "@/lib/utils"
import { Clock, DollarSign, Plus, Sparkles } from "lucide-react"
import { useState } from "react"
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

export function AddActivityModal({ tripId, dayNumber, isOpen, onClose, onSuccess }: AddActivityModalProps) {
  const [title, setTitle] = useState("")
  const [activityType, setActivityType] = useState<ActivityType>("SIGHTSEEING")
  const [startTime, setStartTime] = useState("09:00")
  const [endTime, setEndTime] = useState("11:00")
  const [estimatedCost, setEstimatedCost] = useState("")
  const [notes, setNotes] = useState("")
  const [loading, setLoading] = useState(false)
  const [costHint, setCostHint] = useState<string | null>(null)

  const handleFetchCostHint = async () => {
    try {
      const hint = await itineraryApi.getCostEstimate(tripId, { priceLevel: 2, activityType })
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
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="p-2 rounded-lg bg-primary/10 text-primary">
            <Plus className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle>Thêm Hoạt Động Ngày {dayNumber}</DialogTitle>
            <DialogDescription>Lên lịch chi tiết các điểm đến</DialogDescription>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-col min-h-0">
          <DialogBody className="space-y-4 text-xs">
            <div className="space-y-1.5">
              <label htmlFor="activity-title" className="text-xs font-medium">
                Tên hoạt động / Địa điểm
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
      </DialogContent>
    </Dialog>
  )
}
