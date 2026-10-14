import { adminApi } from "@/client/api/admin-api"
import { getErrorMessage } from "@/client/errors"
import type { DestinationKnowledge, KnowledgeCategory } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { queryKeys } from "@/hooks/query-keys"
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { BookOpen, MapPin, Plus, RotateCw, Search } from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"
import { AdminQueryError } from "./AdminQueryError"
import { KnowledgeFormDialog } from "./KnowledgeFormDialog"
import { KnowledgeTable } from "./KnowledgeTable"
import { KNOWLEDGE_CATEGORIES, KNOWLEDGE_CATEGORY_IDS } from "./knowledge-categories"

const PAGE_SIZE = 15
const FILTER_SELECT =
  "h-8 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"

export function KnowledgeManagementTab() {
  const queryClient = useQueryClient()
  const [selectedDest, setSelectedDest] = useState("ALL")
  const [selectedCat, setSelectedCat] = useState<KnowledgeCategory | "ALL">("ALL")
  const [searchInput, setSearchInput] = useState("")
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(0)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<DestinationKnowledge | null>(null)

  const filters = {
    destination: selectedDest === "ALL" ? undefined : selectedDest,
    category: selectedCat === "ALL" ? undefined : selectedCat,
    search: search || undefined,
    page,
  }

  const { data: destinations = [] } = useQuery({
    queryKey: queryKeys.admin.knowledgeDestinations,
    queryFn: adminApi.getKnowledgeDestinations,
  })

  const listQuery = useQuery({
    queryKey: queryKeys.admin.knowledge(filters),
    queryFn: () => adminApi.getKnowledgeList({ ...filters, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })
  const items = listQuery.data?.content ?? []
  const totalElements = listQuery.data?.totalElements ?? 0

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "knowledge"] })

  const toggleMutation = useMutation({
    mutationFn: (item: DestinationKnowledge) => adminApi.toggleKnowledgeActive(item.id),
    onSuccess: (updated) => {
      toast.success(updated.isActive ? "Đã kích hoạt tri thức cho AI sử dụng" : "Đã tạm dừng sử dụng tri thức này")
      invalidate()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể đổi trạng thái")),
  })

  const deleteMutation = useMutation({
    mutationFn: (item: DestinationKnowledge) => adminApi.deleteKnowledge(item.id),
    onSuccess: () => {
      toast.success("Đã xoá tri thức thành công!")
      invalidate()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể xoá tri thức")),
  })

  const openForm = (item: DestinationKnowledge | null) => {
    setEditingItem(item)
    setIsFormOpen(true)
  }

  const resetFilters = () => {
    setSearchInput("")
    setSearch("")
    setSelectedDest("ALL")
    setSelectedCat("ALL")
    setPage(0)
  }

  return (
    <div className="space-y-4">
      <div className="p-4 rounded-2xl bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-pink-500/10 border border-indigo-500/20 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="p-2.5 rounded-xl bg-indigo-500/20 text-indigo-600 dark:text-indigo-400 shrink-0 mt-0.5">
            <BookOpen className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-sm text-foreground">
                Quản Lý Dataset &amp; Tri Thức Du Lịch (Cộng Đồng &amp; Diễn Đàn)
              </h3>
              <Badge
                variant="secondary"
                className="text-[10px] bg-indigo-500/15 text-indigo-600 dark:text-indigo-400 border-none font-bold"
              >
                Phase 2
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5 max-w-2xl">
              Nạp các kinh nghiệm thực tế, mẹo giờ giấc, cảnh báo tránh bẫy từ Facebook, TikTok, Blog du lịch. Hệ thống
              sẽ tự động <strong>rây lọc và nạp vào AI Prompt (RAG)</strong> để lịch trình do AI gợi ý mang tính chuẩn
              xác và am hiểu địa phương.
            </p>
          </div>
        </div>
        <Button size="sm" onClick={() => openForm(null)} className="gap-1.5 cursor-pointer text-xs shrink-0 shadow-md">
          <Plus className="h-4 w-4" /> Thêm mẹo du lịch mới
        </Button>
      </div>

      <Card className="p-4 border-border/80 shadow-sm">
        <form
          onSubmit={(e) => {
            e.preventDefault()
            setPage(0)
            setSearch(searchInput.trim())
          }}
          className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 text-xs"
        >
          <div className="flex flex-wrap items-center gap-2.5">
            <div className="flex items-center gap-1.5">
              <MapPin className="h-3.5 w-3.5 text-muted-foreground" aria-hidden />
              <select
                aria-label="Lọc theo điểm đến"
                value={selectedDest}
                onChange={(e) => {
                  setSelectedDest(e.target.value)
                  setPage(0)
                }}
                className={FILTER_SELECT}
              >
                <option value="ALL">Tất cả điểm đến ({destinations.length})</option>
                {destinations.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </div>
            <select
              aria-label="Lọc theo chuyên mục"
              value={selectedCat}
              onChange={(e) => {
                setSelectedCat(e.target.value as KnowledgeCategory | "ALL")
                setPage(0)
              }}
              className={FILTER_SELECT}
            >
              <option value="ALL">Tất cả chuyên mục</option>
              {KNOWLEDGE_CATEGORY_IDS.map((key) => (
                <option key={key} value={key}>
                  {KNOWLEDGE_CATEGORIES[key].label}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative flex-1 md:w-64">
              <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" aria-hidden />
              <Input
                aria-label="Tìm tri thức"
                placeholder="Tìm tiêu đề, nội dung, quán..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="pl-8 h-8 text-xs bg-background"
              />
            </div>
            <Button type="submit" size="sm" variant="secondary" className="h-8 text-xs px-3">
              Tìm
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={resetFilters}
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
              aria-label="Đặt lại bộ lọc"
            >
              <RotateCw className="h-3.5 w-3.5" />
            </Button>
          </div>
        </form>
      </Card>

      {listQuery.isError ? (
        <AdminQueryError error={listQuery.error} />
      ) : (
        <Card className="overflow-hidden border-border/80 shadow-sm">
          <div className="p-3.5 border-b bg-muted/30 flex items-center justify-between text-xs">
            <div className="font-semibold text-foreground flex items-center gap-2">
              <span>Danh sách tri thức trong Dataset</span>
              <Badge variant="outline" className="text-[10px] font-mono">
                {totalElements} mục
              </Badge>
            </div>
            {listQuery.isFetching && (
              <div className="flex items-center gap-1.5 text-muted-foreground text-xs">
                <RotateCw className="h-3 w-3 animate-spin text-primary" aria-hidden /> Đang tải...
              </div>
            )}
          </div>

          <KnowledgeTable
            items={items}
            busy={toggleMutation.isPending || deleteMutation.isPending}
            onEdit={openForm}
            onToggle={(item) => toggleMutation.mutate(item)}
            onDelete={(item) => {
              if (window.confirm(`Bạn có chắc muốn xoá tri thức "${item.title}" khỏi dataset?`)) {
                deleteMutation.mutate(item)
              }
            }}
          />

          {totalElements > PAGE_SIZE && (
            <div className="p-3 border-t bg-muted/20 flex items-center justify-between text-xs">
              <span className="text-muted-foreground">
                Hiển thị trang {page + 1} / {Math.ceil(totalElements / PAGE_SIZE)}
              </span>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="h-7 text-xs px-2.5"
                >
                  Trước
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={(page + 1) * PAGE_SIZE >= totalElements}
                  onClick={() => setPage((p) => p + 1)}
                  className="h-7 text-xs px-2.5"
                >
                  Sau
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}

      <KnowledgeFormDialog
        isOpen={isFormOpen}
        item={editingItem}
        defaultDestination={selectedDest !== "ALL" ? selectedDest : destinations[0] || "Da Nang"}
        onClose={() => setIsFormOpen(false)}
      />
    </div>
  )
}
