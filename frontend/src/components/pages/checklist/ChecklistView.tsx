import { checklistApi } from "@/client/api/checklist-api"
import { getErrorMessage } from "@/client/errors"
import type { ChecklistCategory, ChecklistItem, ChecklistKind } from "@/client/types"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { queryKeys } from "@/hooks/query-keys"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { CalendarCheck, Luggage, Plus, Sparkles } from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"
import { ChecklistColumn } from "./ChecklistColumn"
import { ChecklistSuggestionsDialog } from "./ChecklistSuggestionsDialog"

interface ChecklistViewProps {
  tripId: number
}

const CATEGORIES: Array<{ id: ChecklistCategory; label: string }> = [
  { id: "CLOTHING", label: "Quần áo" },
  { id: "TOILETRIES", label: "Đồ vệ sinh" },
  { id: "HEALTH", label: "Sức khỏe" },
  { id: "DOCUMENTS", label: "Giấy tờ" },
  { id: "ELECTRONICS", label: "Điện tử" },
  { id: "MONEY", label: "Tiền" },
  { id: "BOOKING", label: "Đặt chỗ" },
  { id: "KIDS", label: "Trẻ em" },
  { id: "OTHER", label: "Khác" },
]

export function ChecklistView({ tripId }: ChecklistViewProps) {
  const queryClient = useQueryClient()
  const key = queryKeys.checklist(tripId)
  const [newTitle, setNewTitle] = useState("")
  const [newKind, setNewKind] = useState<ChecklistKind>("PACK")
  const [newCategory, setNewCategory] = useState<ChecklistCategory>("CLOTHING")
  const [isSuggestOpen, setIsSuggestOpen] = useState(false)

  const {
    data: items = [],
    isPending,
    isError,
    error,
  } = useQuery({
    queryKey: key,
    queryFn: () => checklistApi.getChecklist(tripId),
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: key })

  const toggleMutation = useMutation({
    mutationFn: (item: ChecklistItem) => checklistApi.updateItem(item.id, { done: !item.done }),
    onSuccess: (updated) =>
      queryClient.setQueryData<ChecklistItem[]>(key, (prev) => prev?.map((i) => (i.id === updated.id ? updated : i))),
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi cập nhật trạng thái")),
  })

  const deleteMutation = useMutation({
    mutationFn: (item: ChecklistItem) => checklistApi.deleteItem(item.id),
    onSuccess: (_res, item) => {
      queryClient.setQueryData<ChecklistItem[]>(key, (prev) => prev?.filter((i) => i.id !== item.id))
      toast.success("Đã xoá mục")
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi xoá mục")),
  })

  const addMutation = useMutation({
    mutationFn: () =>
      checklistApi.createItems(tripId, [{ title: newTitle.trim(), kind: newKind, category: newCategory }]),
    onSuccess: () => {
      setNewTitle("")
      toast.success("Đã thêm vào checklist!")
      invalidate()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể thêm mục")),
  })

  const handleAddItem = (e: React.FormEvent) => {
    e.preventDefault()
    if (newTitle.trim()) addMutation.mutate()
  }

  if (isError) {
    return (
      <div className="py-12 text-center text-xs text-muted-foreground">
        {getErrorMessage(error, "Không thể tải checklist")}
      </div>
    )
  }

  const packItems = items.filter((i) => i.kind === "PACK")
  const todoItems = items.filter((i) => i.kind === "TODO")

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-2xl bg-gradient-to-r from-emerald-500/10 via-teal-500/10 to-sky-500/10 border border-emerald-500/20">
        <div>
          <h4 className="font-bold text-sm text-foreground flex items-center gap-2">
            <Luggage className="h-4 w-4 text-emerald-500" aria-hidden />
            Checklist Chuẩn Bị Trước Chuyến Đi
          </h4>
          <p className="text-xs text-muted-foreground mt-0.5">
            {isPending ? "Đang tải..." : `Đã hoàn thành ${items.filter((i) => i.done).length} / ${items.length} mục`}
          </p>
        </div>

        <Button
          size="sm"
          onClick={() => setIsSuggestOpen(true)}
          className="gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
        >
          <Sparkles className="h-3.5 w-3.5" /> Gợi ý thông minh (AI & Thời tiết)
        </Button>
      </div>

      <Card className="p-4">
        <form onSubmit={handleAddItem} className="flex flex-col sm:flex-row gap-2 sm:items-center text-xs">
          <Input
            aria-label="Tên mục chuẩn bị"
            maxLength={120}
            placeholder="Thêm mục chuẩn bị mới (VD: Mua kem chống nắng, Sạc dự phòng...)"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            className="flex-1"
          />
          <div className="flex items-center gap-2">
            <select
              aria-label="Loại mục"
              className="flex-1 sm:flex-initial rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"
              value={newKind}
              onChange={(e) => setNewKind(e.target.value as ChecklistKind)}
            >
              <option value="PACK">Đồ mang theo</option>
              <option value="TODO">Việc cần làm</option>
            </select>
            <select
              aria-label="Nhóm"
              className="flex-1 sm:flex-initial rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"
              value={newCategory}
              onChange={(e) => setNewCategory(e.target.value as ChecklistCategory)}
            >
              {CATEGORIES.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.label}
                </option>
              ))}
            </select>
            <Button type="submit" size="sm" className="gap-1 shrink-0" disabled={addMutation.isPending}>
              <Plus className="h-3.5 w-3.5" /> Thêm
            </Button>
          </div>
        </form>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <ChecklistColumn
          icon={<Luggage className="h-4 w-4 text-primary" aria-hidden />}
          title="Đồ cần mang"
          doneLabel="đã chuẩn bị"
          emptyText="Chưa có đồ dùng nào trong danh sách."
          items={packItems}
          onToggle={(item) => toggleMutation.mutate(item)}
          onDelete={(item) => deleteMutation.mutate(item)}
        />
        <ChecklistColumn
          icon={<CalendarCheck className="h-4 w-4 text-indigo-500" aria-hidden />}
          title="Việc cần làm"
          doneLabel="đã hoàn tất"
          emptyText="Chưa có việc cần làm nào trong danh sách."
          items={todoItems}
          onToggle={(item) => toggleMutation.mutate(item)}
          onDelete={(item) => deleteMutation.mutate(item)}
        />
      </div>

      <ChecklistSuggestionsDialog
        tripId={tripId}
        isOpen={isSuggestOpen}
        onClose={() => setIsSuggestOpen(false)}
        onAdded={invalidate}
      />
    </div>
  )
}
