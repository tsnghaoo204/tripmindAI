import { checklistApi } from "@/client/api/checklist-api"
import { getErrorMessage } from "@/client/errors"
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
import { useMutation, useQuery } from "@tanstack/react-query"
import { CheckCircle2, Sparkles, Square } from "lucide-react"
import { useEffect, useState } from "react"
import { toast } from "sonner"

interface ChecklistSuggestionsDialogProps {
  tripId: number
  isOpen: boolean
  onClose: () => void
  onAdded: () => void
}

export function ChecklistSuggestionsDialog({ tripId, isOpen, onClose, onAdded }: ChecklistSuggestionsDialogProps) {
  const [selected, setSelected] = useState<Record<number, boolean>>({})
  const { data, isFetching } = useQuery({
    queryKey: ["trip", tripId, "checklist-suggestions"],
    queryFn: () => checklistApi.getSuggestions(tripId),
    enabled: isOpen,
    staleTime: 0,
  })
  const items = data?.items ?? []

  // Pre-select every suggestion whenever a new list arrives
  useEffect(() => {
    setSelected(Object.fromEntries((data?.items ?? []).map((_, idx) => [idx, true])))
  }, [data])

  const addMutation = useMutation({
    mutationFn: () =>
      checklistApi.createItems(
        tripId,
        items
          .filter((_, idx) => selected[idx])
          .map((s) => ({
            title: s.title,
            kind: s.kind,
            category: s.category,
            dueDate: s.dueDate,
            reason: s.reason,
            source: "SUGGESTED" as const,
          })),
      ),
    onSuccess: (created) => {
      toast.success(`Đã thêm ${created.length} mục vào checklist!`)
      onAdded()
      onClose()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Có lỗi khi thêm gợi ý")),
  })

  const handleApply = () => {
    if (items.some((_, idx) => selected[idx])) addMutation.mutate()
    else onClose()
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-600">
            <Sparkles className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle className="text-sm">Gợi ý từ Rule Engine & Thời tiết</DialogTitle>
            <DialogDescription>Dựa trên dự báo mưa, bãi biển, chuyến bay & nhóm đi</DialogDescription>
          </div>
        </DialogHeader>

        <DialogBody className="space-y-2 text-xs">
          {isFetching ? (
            <div className="py-8 text-center text-muted-foreground">Đang tính toán theo thời tiết và hoạt động...</div>
          ) : items.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">Không có gợi ý thêm nào cho chuyến đi này.</div>
          ) : (
            items.map((s, idx) => (
              <button
                type="button"
                aria-pressed={!!selected[idx]}
                key={`${s.kind}-${s.title}`}
                onClick={() => setSelected((prev) => ({ ...prev, [idx]: !prev[idx] }))}
                className={`w-full text-left p-3 rounded-xl border transition-all cursor-pointer flex items-start gap-3 ${
                  selected[idx]
                    ? "border-emerald-500/50 bg-emerald-500/5 text-foreground"
                    : "border-border text-muted-foreground opacity-60"
                }`}
              >
                {selected[idx] ? (
                  <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0 mt-0.5" aria-hidden />
                ) : (
                  <Square className="h-4 w-4 text-muted-foreground shrink-0 mt-0.5" aria-hidden />
                )}
                <div className="space-y-1">
                  <div className="font-semibold text-xs flex items-center gap-2">
                    <span>{s.title}</span>
                    <Badge variant="outline" className="text-[10px] h-4">
                      {s.kind === "PACK" ? "Đồ mang" : "Cần làm"}
                    </Badge>
                  </div>
                  {s.reason && <p className="text-[11px] text-muted-foreground italic">Lý do: {s.reason}</p>}
                </div>
              </button>
            ))
          )}
        </DialogBody>

        <DialogFooter>
          <Button variant="outline" size="sm" onClick={onClose}>
            Hủy
          </Button>
          <Button
            size="sm"
            onClick={handleApply}
            disabled={addMutation.isPending || isFetching}
            className="bg-emerald-600 hover:bg-emerald-700 text-white"
          >
            Thêm mục đã chọn vào checklist
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
