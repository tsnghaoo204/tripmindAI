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
import { Copy } from "lucide-react"
import { useEffect, useState } from "react"

interface DuplicateTripModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  defaultDate?: string
  isLoading?: boolean
  onDuplicate: (startDate: string) => void
}

export function DuplicateTripModal({
  open,
  onOpenChange,
  defaultDate = "",
  isLoading = false,
  onDuplicate,
}: DuplicateTripModalProps) {
  const [startDate, setStartDate] = useState(defaultDate)

  useEffect(() => {
    if (open) {
      setStartDate(defaultDate)
    }
  }, [open, defaultDate])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!startDate.trim()) return
    onDuplicate(startDate.trim())
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-primary/10 text-primary shrink-0">
                <Copy className="h-5 w-5" aria-hidden />
              </div>
              <div>
                <DialogTitle>Nhân bản chuyến đi</DialogTitle>
                <DialogDescription className="mt-1">
                  Sao chép lịch trình hiện tại sang một chuyến mới với ngày khởi hành bạn chọn.
                </DialogDescription>
              </div>
            </div>
          </DialogHeader>

          <DialogBody className="space-y-4 py-4">
            <div className="space-y-1.5">
              <label htmlFor="duplicate-date" className="text-sm font-medium">
                Ngày bắt đầu mới (YYYY-MM-DD)
              </label>
              <Input
                id="duplicate-date"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
              />
              <p className="text-xs text-muted-foreground">
                Tất cả hoạt động và chi tiêu sẽ được điều chỉnh theo mốc thời gian mới.
              </p>
            </div>
          </DialogBody>

          <DialogFooter className="gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
              Hủy
            </Button>
            <Button type="submit" disabled={isLoading || !startDate.trim()}>
              {isLoading ? "Đang nhân bản..." : "Nhân bản chuyến đi"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
