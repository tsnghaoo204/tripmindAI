import React, { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { X, Plus, Clock, DollarSign, Sparkles } from "lucide-react"
import { toast } from "sonner"
import { itineraryApi } from "@/client/api/itinerary-api"
import { formatCurrency } from "@/lib/utils"

interface AddActivityModalProps {
  tripId: number
  dayNumber: number
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

const ACTIVITY_TYPES = [
  { id: "SIGHTSEEING", label: "Tham quan / Thắng cảnh" },
  { id: "FOOD", label: "Ăn uống / Nhà hàng" },
  { id: "ACCOMMODATION", label: "Nghỉ ngơi / Khách sạn" },
  { id: "TRANSPORT", label: "Di chuyển" },
  { id: "REST", label: "Nghỉ ngơi tự do" },
  { id: "OTHER", label: "Khác" },
]

export function AddActivityModal({
  tripId,
  dayNumber,
  isOpen,
  onClose,
  onSuccess,
}: AddActivityModalProps) {
  const [title, setTitle] = useState("")
  const [activityType, setActivityType] = useState("SIGHTSEEING")
  const [startTime, setStartTime] = useState("09:00")
  const [endTime, setEndTime] = useState("11:00")
  const [estimatedCost, setEstimatedCost] = useState("")
  const [notes, setNotes] = useState("")
  const [loading, setLoading] = useState(false)
  const [costHint, setCostHint] = useState<string | null>(null)

  if (!isOpen) return null

  const handleFetchCostHint = async () => {
    try {
      const hint = await itineraryApi.getCostEstimate(tripId, {
        dayNumber,
        activityType,
      })
      if (hint.estimatedCost) {
        setEstimatedCost(String(hint.estimatedCost))
        setCostHint(hint.explanation)
        toast.info(`Gợi ý: ${formatCurrency(hint.estimatedCost)}`)
      }
    } catch {
      toast.error("Không tìm thấy ước tính giá cho mục này")
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
        startTime,
        endTime,
        estimatedCost: estimatedCost ? Number(estimatedCost) : undefined,
        notes,
      })
      toast.success("Đã thêm hoạt động vào lịch trình!")
      onSuccess()
      onClose()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi thêm hoạt động")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
      <Card className="w-full max-w-md shadow-2xl border-border animate-in fade-in zoom-in-95">
        <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Plus className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-base font-bold">Thêm Hoạt Động Ngày {dayNumber}</CardTitle>
              <p className="text-xs text-muted-foreground">Lên lịch chi tiết các điểm đến</p>
            </div>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} className="rounded-full">
            <X className="h-4 w-4" />
          </Button>
        </CardHeader>

        <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
          <div className="space-y-1.5">
            <label className="text-xs font-medium">Tên hoạt động / Địa điểm</label>
            <Input
              required
              placeholder="VD: Cà phê sáng ngắm biển Mỹ Khê"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-medium">Phân loại</label>
            <select
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs focus:ring-2 focus:ring-ring outline-none"
              value={activityType}
              onChange={(e) => setActivityType(e.target.value)}
            >
              {ACTIVITY_TYPES.map((t) => (
                <option key={t.id} value={t.id}>{t.label}</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="text-xs font-medium flex items-center gap-1">
                <Clock className="h-3.5 w-3.5 text-primary" /> Bắt đầu
              </label>
              <Input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium flex items-center gap-1">
                <Clock className="h-3.5 w-3.5 text-primary" /> Kết thúc
              </label>
              <Input
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <div className="flex justify-between items-center">
              <label className="text-xs font-medium flex items-center gap-1">
                <DollarSign className="h-3.5 w-3.5 text-emerald-500" /> Chi phí ước tính (VND)
              </label>
              <button
                type="button"
                onClick={handleFetchCostHint}
                className="text-[11px] text-primary hover:underline flex items-center gap-1 cursor-pointer"
              >
                <Sparkles className="h-3 w-3" /> Gợi ý giá
              </button>
            </div>
            <Input
              type="number"
              placeholder="VD: 150000"
              value={estimatedCost}
              onChange={(e) => setEstimatedCost(e.target.value)}
            />
            {costHint && <p className="text-[10px] text-muted-foreground">{costHint}</p>}
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-medium">Ghi chú</label>
            <Input
              placeholder="VD: Nhớ mang máy ảnh, đặt bàn trước..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          <div className="pt-2 flex justify-end gap-2 border-t">
            <Button type="button" variant="outline" size="sm" onClick={onClose}>Hủy</Button>
            <Button type="submit" size="sm" disabled={loading}>
              {loading ? "Đang lưu..." : "Thêm vào ngày"}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
