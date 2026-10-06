import React, { useState, useEffect } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  CheckSquare,
  Square,
  Plus,
  Trash2,
  Sparkles,
  Luggage,
  CalendarCheck,
  CheckCircle2,
  X,
} from "lucide-react"
import { checklistApi, type ChecklistSuggestionsResponse } from "@/client/api/checklist-api"
import type { ChecklistItem } from "@/client/types"
import { toast } from "sonner"

interface ChecklistViewProps {
  tripId: number
}

export function ChecklistView({ tripId }: ChecklistViewProps) {
  const [items, setItems] = useState<ChecklistItem[]>([])
  const [loading, setLoading] = useState(true)

  // Add Item State
  const [newTitle, setNewTitle] = useState("")
  const [newKind, setNewKind] = useState<"PACK" | "TODO">("PACK")
  const [newCategory, setNewCategory] = useState("CLOTHING")

  // Suggestions Drawer
  const [isSuggestModalOpen, setIsSuggestModalOpen] = useState(false)
  const [suggestions, setSuggestions] = useState<ChecklistSuggestionsResponse | null>(null)
  const [selectedSuggestions, setSelectedSuggestions] = useState<Record<number, boolean>>({})
  const [loadingSuggestions, setLoadingSuggestions] = useState(false)

  const fetchItems = async () => {
    try {
      setLoading(true)
      const data = await checklistApi.getChecklist(tripId)
      setItems(data)
    } catch {
      toast.error("Không thể tải checklist")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchItems()
  }, [tripId])

  const handleToggleComplete = async (item: ChecklistItem) => {
    try {
      const updated = await checklistApi.updateItem(item.id, {
        isCompleted: !item.isCompleted,
      })
      setItems((prev) => prev.map((i) => (i.id === item.id ? updated : i)))
    } catch {
      toast.error("Lỗi khi cập nhật trạng thái")
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await checklistApi.deleteItem(id)
      setItems((prev) => prev.filter((i) => i.id !== id))
      toast.success("Đã xoá mục")
    } catch {
      toast.error("Lỗi khi xoá mục")
    }
  }

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newTitle.trim()) return
    try {
      const created = await checklistApi.createItem(tripId, {
        title: newTitle.trim(),
        kind: newKind,
        category: newCategory,
      })
      setItems((prev) => [...prev, created])
      setNewTitle("")
      toast.success("Đã thêm vào checklist!")
    } catch {
      toast.error("Không thể thêm mục")
    }
  }

  const handleOpenSuggestions = async () => {
    setIsSuggestModalOpen(true)
    setLoadingSuggestions(true)
    try {
      const res = await checklistApi.getSuggestions(tripId)
      setSuggestions(res)
      // Mặc định chọn tất cả gợi ý
      const initialSelected: Record<number, boolean> = {}
      res.items.forEach((_, idx) => {
        initialSelected[idx] = true
      })
      setSelectedSuggestions(initialSelected)
    } catch {
      toast.error("Lỗi khi lấy danh sách gợi ý")
    } finally {
      setLoadingSuggestions(false)
    }
  }

  const handleApplySuggestions = async () => {
    if (!suggestions) return
    const itemsToAdd = suggestions.items.filter((_, idx) => selectedSuggestions[idx])
    if (itemsToAdd.length === 0) {
      setIsSuggestModalOpen(false)
      return
    }

    try {
      for (const item of itemsToAdd) {
        await checklistApi.createItem(tripId, {
          title: item.title,
          kind: item.kind,
          category: item.category,
          dueDate: item.dueDate,
        })
      }
      toast.success(`Đã thêm ${itemsToAdd.length} mục vào checklist!`)
      setIsSuggestModalOpen(false)
      fetchItems()
    } catch {
      toast.error("Có lỗi khi thêm gợi ý")
    }
  }

  const packItems = items.filter((i) => i.kind === "PACK")
  const todoItems = items.filter((i) => i.kind === "TODO")

  return (
    <div className="space-y-6">
      {/* Top Banner & AI Suggestion Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-2xl bg-gradient-to-r from-emerald-500/10 via-teal-500/10 to-sky-500/10 border border-emerald-500/20">
        <div>
          <h4 className="font-bold text-sm text-foreground flex items-center gap-2">
            <Luggage className="h-4 w-4 text-emerald-500" />
            Checklist Chuẩn Bị Trước Chuyến Đi
          </h4>
          <p className="text-xs text-muted-foreground mt-0.5">
            Đã hoàn thành {items.filter((i) => i.isCompleted).length} / {items.length} mục
          </p>
        </div>

        <Button size="sm" onClick={handleOpenSuggestions} className="gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white">
          <Sparkles className="h-3.5 w-3.5" /> Gợi ý thông minh (AI & Thời tiết)
        </Button>
      </div>

      {/* Quick Add Form */}
      <Card className="p-4">
        <form onSubmit={handleAddItem} className="flex flex-wrap sm:flex-nowrap gap-2 items-center text-xs">
          <Input
            placeholder="Thêm mục chuẩn bị mới (VD: Mua kem chống nắng, Sạc dự phòng...)"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            className="flex-1"
          />
          <select
            className="rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"
            value={newKind}
            onChange={(e) => setNewKind(e.target.value as any)}
          >
            <option value="PACK">Đồ mang theo</option>
            <option value="TODO">Việc cần làm</option>
          </select>
          <Button type="submit" size="sm" className="gap-1 shrink-0">
            <Plus className="h-3.5 w-3.5" /> Thêm
          </Button>
        </form>
      </Card>

      {/* 2 Columns: PACK and TODO */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* PACK Items */}
        <Card className="p-5 space-y-3">
          <div className="flex items-center justify-between border-b pb-2">
            <div className="flex items-center gap-2 font-bold text-sm text-foreground">
              <Luggage className="h-4 w-4 text-primary" />
              <span>Đồ cần mang ({packItems.length})</span>
            </div>
            <span className="text-[11px] text-muted-foreground">
              {packItems.filter((i) => i.isCompleted).length} đã chuẩn bị
            </span>
          </div>

          {packItems.length === 0 ? (
            <p className="text-xs text-muted-foreground py-6 text-center">Chưa có đồ dùng nào trong danh sách.</p>
          ) : (
            <div className="divide-y divide-border/60">
              {packItems.map((item) => (
                <div key={item.id} className="py-2.5 flex items-center justify-between gap-3 text-xs group">
                  <button
                    type="button"
                    onClick={() => handleToggleComplete(item)}
                    className="flex items-center gap-2.5 text-left flex-1 cursor-pointer"
                  >
                    {item.isCompleted ? (
                      <CheckSquare className="h-4 w-4 text-emerald-500 shrink-0" />
                    ) : (
                      <Square className="h-4 w-4 text-muted-foreground shrink-0" />
                    )}
                    <span className={`${item.isCompleted ? "line-through text-muted-foreground" : "text-foreground font-medium"}`}>
                      {item.title}
                    </span>
                  </button>

                  <Button
                    size="icon"
                    variant="ghost"
                    onClick={() => handleDelete(item.id)}
                    className="h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground hover:text-destructive"
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* TODO Items */}
        <Card className="p-5 space-y-3">
          <div className="flex items-center justify-between border-b pb-2">
            <div className="flex items-center gap-2 font-bold text-sm text-foreground">
              <CalendarCheck className="h-4 w-4 text-indigo-500" />
              <span>Việc cần làm ({todoItems.length})</span>
            </div>
            <span className="text-[11px] text-muted-foreground">
              {todoItems.filter((i) => i.isCompleted).length} đã hoàn tất
            </span>
          </div>

          {todoItems.length === 0 ? (
            <p className="text-xs text-muted-foreground py-6 text-center">Chưa có việc cần làm nào trong danh sách.</p>
          ) : (
            <div className="divide-y divide-border/60">
              {todoItems.map((item) => (
                <div key={item.id} className="py-2.5 flex items-center justify-between gap-3 text-xs group">
                  <button
                    type="button"
                    onClick={() => handleToggleComplete(item)}
                    className="flex items-center gap-2.5 text-left flex-1 cursor-pointer"
                  >
                    {item.isCompleted ? (
                      <CheckSquare className="h-4 w-4 text-emerald-500 shrink-0" />
                    ) : (
                      <Square className="h-4 w-4 text-muted-foreground shrink-0" />
                    )}
                    <div className="space-y-0.5">
                      <span className={`${item.isCompleted ? "line-through text-muted-foreground" : "text-foreground font-medium"}`}>
                        {item.title}
                      </span>
                      {item.dueDate && (
                        <span className="block text-[10px] text-muted-foreground">Hạn chót: {item.dueDate}</span>
                      )}
                    </div>
                  </button>

                  <Button
                    size="icon"
                    variant="ghost"
                    onClick={() => handleDelete(item.id)}
                    className="h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground hover:text-destructive"
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* AI Suggestion Modal */}
      {isSuggestModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
          <Card className="w-full max-w-lg shadow-2xl border-border animate-in fade-in zoom-in-95 max-h-[85vh] flex flex-col">
            <div className="p-4 border-b flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-600">
                  <Sparkles className="h-5 w-5" />
                </div>
                <div>
                  <h4 className="font-bold text-sm">Gợi ý từ Rule Engine & Thời tiết</h4>
                  <p className="text-[11px] text-muted-foreground">Dựa trên dự báo mưa, bãi biển, chuyến bay & nhóm đi</p>
                </div>
              </div>
              <Button variant="ghost" size="icon" onClick={() => setIsSuggestModalOpen(false)}>
                <X className="h-4 w-4" />
              </Button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-2 text-xs">
              {loadingSuggestions ? (
                <div className="py-8 text-center text-muted-foreground">Đang tính toán theo thời tiết và hoạt động...</div>
              ) : !suggestions || suggestions.items.length === 0 ? (
                <div className="py-8 text-center text-muted-foreground">Không có gợi ý thêm nào cho chuyến đi này.</div>
              ) : (
                suggestions.items.map((s, idx) => (
                  <div
                    key={idx}
                    onClick={() =>
                      setSelectedSuggestions((prev) => ({ ...prev, [idx]: !prev[idx] }))
                    }
                    className={`p-3 rounded-xl border transition-all cursor-pointer flex items-start gap-3 ${
                      selectedSuggestions[idx]
                        ? "border-emerald-500/50 bg-emerald-500/5 text-foreground"
                        : "border-border text-muted-foreground opacity-60"
                    }`}
                  >
                    {selectedSuggestions[idx] ? (
                      <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0 mt-0.5" />
                    ) : (
                      <Square className="h-4 w-4 text-muted-foreground shrink-0 mt-0.5" />
                    )}
                    <div className="space-y-1">
                      <div className="font-semibold text-xs flex items-center gap-2">
                        <span>{s.title}</span>
                        <Badge variant="outline" className="text-[10px] h-4">
                          {s.kind === "PACK" ? "Đồ mang" : "Cần làm"}
                        </Badge>
                      </div>
                      <p className="text-[11px] text-muted-foreground italic">Lý do: {s.reason}</p>
                    </div>
                  </div>
                ))
              )}
            </div>

            <div className="p-4 border-t flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setIsSuggestModalOpen(false)}>
                Hủy
              </Button>
              <Button size="sm" onClick={handleApplySuggestions} className="bg-emerald-600 hover:bg-emerald-700 text-white">
                Thêm mục đã chọn vào checklist
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}
