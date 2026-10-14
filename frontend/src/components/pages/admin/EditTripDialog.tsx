import { adminApi } from "@/client/api/admin-api"
import { getErrorMessage } from "@/client/errors"
import type { AdminTripRow, TripPhase } from "@/client/types"
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
import { queryKeys } from "@/hooks/query-keys"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Pencil } from "lucide-react"
import { useEffect, useState } from "react"
import { toast } from "sonner"

export function EditTripDialog({ trip, onClose }: { trip: AdminTripRow | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState("")
  const [phase, setPhase] = useState<TripPhase>("BEFORE")
  const [budget, setBudget] = useState("")

  useEffect(() => {
    if (!trip) return
    setName(trip.name || "")
    setPhase(trip.phase || "BEFORE")
    setBudget("")
  }, [trip])

  const updateMutation = useMutation({
    mutationFn: (tripId: number) =>
      adminApi.updateTrip(tripId, { name: name.trim(), phase, budget: budget ? Number(budget) : undefined }),
    onSuccess: () => {
      toast.success("Đã cập nhật thông tin chuyến đi!")
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.trips })
      onClose()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi cập nhật chuyến đi")),
  })

  return (
    <Dialog open={trip != null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="p-2 rounded-lg bg-amber-500/10 text-amber-500">
            <Pencil className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle>Chỉnh Sửa Kế Hoạch #{trip?.id}</DialogTitle>
            <DialogDescription>{trip?.destinationName}</DialogDescription>
          </div>
        </DialogHeader>

        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (trip) updateMutation.mutate(trip.id)
          }}
          className="flex flex-col min-h-0"
        >
          <DialogBody className="space-y-4 text-xs">
            <div className="space-y-1.5">
              <label htmlFor="edit-trip-name" className="text-xs font-medium">
                Tên chuyến đi
              </label>
              <Input
                id="edit-trip-name"
                required
                maxLength={160}
                placeholder="VD: Du lịch Đà Nẵng 4N3Đ"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <label htmlFor="edit-trip-phase" className="text-xs font-medium">
                Giai đoạn chuyến đi (Phase)
              </label>
              <select
                id="edit-trip-phase"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"
                value={phase}
                onChange={(e) => setPhase(e.target.value as TripPhase)}
              >
                <option value="BEFORE">SẮP TỚI (BEFORE)</option>
                <option value="DURING">ĐANG DIỄN RA (DURING)</option>
                <option value="AFTER">ĐÃ HOÀN THÀNH (AFTER)</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label htmlFor="edit-trip-budget" className="text-xs font-medium">
                Ngân sách dự kiến (VND, bỏ trống nếu giữ nguyên)
              </label>
              <Input
                id="edit-trip-budget"
                type="number"
                min={0}
                placeholder="VD: 5000000"
                value={budget}
                onChange={(e) => setBudget(e.target.value)}
              />
            </div>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="outline" size="sm" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" size="sm" disabled={updateMutation.isPending}>
              {updateMutation.isPending ? "Đang lưu..." : "Lưu cập nhật"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
