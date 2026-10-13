import { useState, useEffect } from "react"
import { adminApi } from "@/client/api/admin-api"
import type { DestinationKnowledge, KnowledgeCategory, KnowledgeRequest } from "@/client/types"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  BookOpen,
  Plus,
  Pencil,
  Trash2,
  Search,
  RotateCw,
  ExternalLink,
  MapPin,
  Utensils,
  Bed,
  Bus,
  Camera,
  Lightbulb,
  AlertOctagon,
  Sparkles,
  Star,
  CheckCircle2,
  XCircle,
  X,
  Tag,
} from "lucide-react"
import { toast } from "sonner"

const CATEGORY_MAP: Record<
  KnowledgeCategory,
  { label: string; icon: any; color: string; badgeClass: string }
> = {
  FOOD: {
    label: "Ăn uống",
    icon: Utensils,
    color: "text-amber-500",
    badgeClass: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20",
  },
  ACCOMMODATION: {
    label: "Lưu trú",
    icon: Bed,
    color: "text-purple-500",
    badgeClass: "bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20",
  },
  TRANSPORT: {
    label: "Đi lại",
    icon: Bus,
    color: "text-blue-500",
    badgeClass: "bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20",
  },
  SIGHTSEEING: {
    label: "Tham quan",
    icon: Camera,
    color: "text-emerald-500",
    badgeClass: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20",
  },
  TIP: {
    label: "Mẹo hay",
    icon: Lightbulb,
    color: "text-yellow-500",
    badgeClass: "bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 border-yellow-500/20",
  },
  WARNING: {
    label: "Cảnh báo / Tránh bẫy",
    icon: AlertOctagon,
    color: "text-rose-500",
    badgeClass: "bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20",
  },
}

export function KnowledgeManagementTab() {
  const [knowledgeList, setKnowledgeList] = useState<DestinationKnowledge[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [destinations, setDestinations] = useState<string[]>([])
  const [loading, setLoading] = useState(false)

  // Filters
  const [selectedDest, setSelectedDest] = useState<string>("ALL")
  const [selectedCat, setSelectedCat] = useState<string>("ALL")
  const [searchQuery, setSearchQuery] = useState<string>("")
  const [page, setPage] = useState(0)
  const pageSize = 15

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<DestinationKnowledge | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Form State
  const [formDestName, setFormDestName] = useState("")
  const [formCategory, setFormCategory] = useState<KnowledgeCategory>("FOOD")
  const [formTitle, setFormTitle] = useState("")
  const [formContent, setFormContent] = useState("")
  const [formPlaceName, setFormPlaceName] = useState("")
  const [formSourceName, setFormSourceName] = useState("")
  const [formSourceUrl, setFormSourceUrl] = useState("")
  const [formTagsStr, setFormTagsStr] = useState("")
  const [formPriority, setFormPriority] = useState<number>(2)
  const [formIsActive, setFormIsActive] = useState<boolean>(true)

  useEffect(() => {
    loadDestinations()
    loadKnowledge()
  }, [])

  useEffect(() => {
    loadKnowledge()
  }, [selectedDest, selectedCat, page])

  const loadDestinations = async () => {
    try {
      const dests = await adminApi.getKnowledgeDestinations()
      setDestinations(dests || [])
    } catch {
      // ignore
    }
  }

  const loadKnowledge = async () => {
    try {
      setLoading(true)
      const res = await adminApi.getKnowledgeList({
        destination: selectedDest === "ALL" ? undefined : selectedDest,
        category: selectedCat === "ALL" ? undefined : selectedCat,
        search: searchQuery.trim() || undefined,
        page,
        size: pageSize,
      })
      setKnowledgeList(res.content || [])
      setTotalElements(res.totalElements || 0)
    } catch {
      toast.error("Không thể tải danh sách tri thức du lịch")
    } finally {
      setLoading(false)
    }
  }

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadKnowledge()
  }

  const handleOpenCreate = () => {
    setEditingItem(null)
    setFormDestName(selectedDest !== "ALL" ? selectedDest : destinations[0] || "Da Nang")
    setFormCategory("FOOD")
    setFormTitle("")
    setFormContent("")
    setFormPlaceName("")
    setFormSourceName("Facebook Review Du Lịch")
    setFormSourceUrl("")
    setFormTagsStr("")
    setFormPriority(2)
    setFormIsActive(true)
    setIsModalOpen(true)
  }

  const handleOpenEdit = (item: DestinationKnowledge) => {
    setEditingItem(item)
    setFormDestName(item.destinationName)
    setFormCategory(item.category)
    setFormTitle(item.title)
    setFormContent(item.content)
    setFormPlaceName(item.placeName || "")
    setFormSourceName(item.sourceName || "")
    setFormSourceUrl(item.sourceUrl || "")
    setFormTagsStr(item.tags ? item.tags.join(", ") : "")
    setFormPriority(item.priority || 1)
    setFormIsActive(item.isActive)
    setIsModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formDestName.trim() || !formTitle.trim() || !formContent.trim()) {
      toast.error("Vui lòng điền đầy đủ Điểm đến, Tiêu đề và Nội dung")
      return
    }

    const tags = formTagsStr
      .split(",")
      .map((t) => t.trim())
      .filter((t) => t.length > 0)

    const payload: KnowledgeRequest = {
      destinationName: formDestName.trim(),
      category: formCategory,
      title: formTitle.trim(),
      content: formContent.trim(),
      placeName: formPlaceName.trim() || undefined,
      sourceName: formSourceName.trim() || undefined,
      sourceUrl: formSourceUrl.trim() || undefined,
      tags,
      priority: formPriority,
      active: formIsActive,
    }

    try {
      setIsSubmitting(true)
      if (editingItem) {
        await adminApi.updateKnowledge(editingItem.id, payload)
        toast.success("Đã cập nhật tri thức du lịch thành công!")
      } else {
        await adminApi.createKnowledge(payload)
        toast.success("Đã thêm tri thức du lịch mới vào dataset!")
      }
      setIsModalOpen(false)
      loadKnowledge()
      loadDestinations()
    } catch {
      toast.error("Không thể lưu tri thức. Vui lòng kiểm tra lại.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleToggleActive = async (id: number) => {
    try {
      const updated = await adminApi.toggleKnowledgeActive(id)
      setKnowledgeList((prev) =>
        prev.map((item) => (item.id === id ? { ...item, isActive: updated.isActive } : item))
      )
      toast.success(
        updated.isActive
          ? "Đã kích hoạt tri thức cho AI sử dụng"
          : "Đã tạm dừng sử dụng tri thức này"
      )
    } catch {
      toast.error("Không thể đổi trạng thái")
    }
  }

  const handleDelete = async (id: number, title: string) => {
    if (!window.confirm(`Bạn có chắc muốn xoá tri thức "${title}" khỏi dataset?`)) {
      return
    }
    try {
      await adminApi.deleteKnowledge(id)
      toast.success("Đã xoá tri thức thành công!")
      setKnowledgeList((prev) => prev.filter((item) => item.id !== id))
      setTotalElements((prev) => Math.max(0, prev - 1))
    } catch {
      toast.error("Không thể xoá tri thức")
    }
  }

  return (
    <div className="space-y-4">
      {/* Banner Giới thiệu Dataset */}
      <div className="p-4 rounded-2xl bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-pink-500/10 border border-indigo-500/20 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="p-2.5 rounded-xl bg-indigo-500/20 text-indigo-600 dark:text-indigo-400 shrink-0 mt-0.5">
            <BookOpen className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-sm text-foreground">
                Quản Lý Dataset &amp; Tri Thức Du Lịch (Cộng Đồng &amp; Diễn Đàn)
              </h3>
              <Badge variant="secondary" className="text-[10px] bg-indigo-500/15 text-indigo-600 dark:text-indigo-400 border-none font-bold">
                Phase 2
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5 max-w-2xl">
              Nạp các kinh nghiệm thực tế, mẹo giờ giấc, cảnh báo tránh bẫy từ Facebook, TikTok, Blog du lịch.
              Hệ thống sẽ tự động <strong>rây lọc và nạp vào AI Prompt (RAG)</strong> để lịch trình do AI gợi ý
              mang tính chuẩn xác và am hiểu địa phương.
            </p>
          </div>
        </div>
        <Button size="sm" onClick={handleOpenCreate} className="gap-1.5 cursor-pointer text-xs shrink-0 shadow-md">
          <Plus className="h-4 w-4" /> Thêm mẹo du lịch mới
        </Button>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4 border-border/80 shadow-sm">
        <form onSubmit={handleSearchSubmit} className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 text-xs">
          <div className="flex flex-wrap items-center gap-2.5">
            {/* Điểm đến */}
            <div className="flex items-center gap-1.5">
              <MapPin className="h-3.5 w-3.5 text-muted-foreground" />
              <select
                value={selectedDest}
                onChange={(e) => {
                  setSelectedDest(e.target.value)
                  setPage(0)
                }}
                className="h-8 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"
              >
                <option value="ALL">Tất cả điểm đến ({destinations.length})</option>
                {destinations.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </div>

            {/* Chuyên mục */}
            <select
              value={selectedCat}
              onChange={(e) => {
                setSelectedCat(e.target.value)
                setPage(0)
              }}
              className="h-8 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"
            >
              <option value="ALL">Tất cả chuyên mục</option>
              {Object.entries(CATEGORY_MAP).map(([key, value]) => (
                <option key={key} value={key}>
                  {value.label}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative flex-1 md:w-64">
              <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
              <Input
                placeholder="Tìm tiêu đề, nội dung, quán..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
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
              onClick={() => {
                setSearchQuery("")
                setSelectedDest("ALL")
                setSelectedCat("ALL")
                setPage(0)
                loadKnowledge()
              }}
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
              title="Đặt lại bộ lọc"
            >
              <RotateCw className="h-3.5 w-3.5" />
            </Button>
          </div>
        </form>
      </Card>

      {/* Knowledge Items Table */}
      <Card className="overflow-hidden border-border/80 shadow-sm">
        <div className="p-3.5 border-b bg-muted/30 flex items-center justify-between text-xs">
          <div className="font-semibold text-foreground flex items-center gap-2">
            <span>Danh sách tri thức trong Dataset</span>
            <Badge variant="outline" className="text-[10px] font-mono">
              {totalElements} mục
            </Badge>
          </div>
          {loading && (
            <div className="flex items-center gap-1.5 text-muted-foreground text-xs">
              <RotateCw className="h-3 w-3 animate-spin text-primary" /> Đang tải...
            </div>
          )}
        </div>

        <div className="overflow-x-auto text-xs">
          <table className="w-full text-left">
            <thead className="bg-muted/50 text-muted-foreground font-semibold">
              <tr>
                <th className="p-3 w-12 text-center">ID</th>
                <th className="p-3 w-32">Điểm đến</th>
                <th className="p-3 w-36">Chuyên mục</th>
                <th className="p-3 min-w-[280px]">Nội dung mẹo &amp; Kinh nghiệm</th>
                <th className="p-3 w-40">Nguồn tham khảo</th>
                <th className="p-3 w-28 text-center">Ưu tiên AI</th>
                <th className="p-3 w-28 text-center">Bật cho AI</th>
                <th className="p-3 w-24 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/40">
              {knowledgeList.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-12 text-center text-muted-foreground">
                    <BookOpen className="h-8 w-8 mx-auto mb-2 opacity-30" />
                    <p className="font-medium">Chưa có tri thức nào phù hợp với bộ lọc.</p>
                    <p className="text-[11px] mt-1">Bấm "Thêm mẹo du lịch mới" để nạp dữ liệu đầu tiên!</p>
                  </td>
                </tr>
              ) : (
                knowledgeList.map((item) => {
                  const catConfig = CATEGORY_MAP[item.category] || CATEGORY_MAP.TIP
                  const Icon = catConfig.icon
                  return (
                    <tr key={item.id} className="hover:bg-muted/20 transition-colors">
                      <td className="p-3 font-mono font-medium text-muted-foreground text-center">
                        {item.id}
                      </td>

                      <td className="p-3 font-semibold text-foreground whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <MapPin className="h-3.5 w-3.5 text-primary shrink-0" />
                          <span>{item.destinationName}</span>
                        </div>
                      </td>

                      <td className="p-3 whitespace-nowrap">
                        <Badge variant="outline" className={`text-[10px] px-2 py-0.5 gap-1 font-semibold ${catConfig.badgeClass}`}>
                          <Icon className="h-3 w-3" />
                          {catConfig.label}
                        </Badge>
                      </td>

                      <td className="p-3">
                        <div className="font-semibold text-foreground text-xs mb-1">
                          {item.title}
                        </div>
                        <p className="text-muted-foreground text-[11px] line-clamp-2 leading-relaxed">
                          {item.content}
                        </p>
                        <div className="flex flex-wrap items-center gap-1.5 mt-1.5">
                          {item.placeName && (
                            <span className="inline-flex items-center gap-1 text-[10px] font-medium text-primary bg-primary/10 px-1.5 py-0.5 rounded">
                              📍 {item.placeName}
                            </span>
                          )}
                          {item.tags &&
                            item.tags.map((t, idx) => (
                              <span
                                key={idx}
                                className="inline-flex items-center gap-0.5 text-[10px] text-muted-foreground bg-muted px-1.5 py-0.5 rounded"
                              >
                                #{t}
                              </span>
                            ))}
                        </div>
                      </td>

                      <td className="p-3 text-[11px] text-muted-foreground">
                        {item.sourceName ? (
                          <div className="font-medium text-foreground flex items-center gap-1">
                            <span>{item.sourceName}</span>
                            {item.sourceUrl && (
                              <a
                                href={item.sourceUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="text-primary hover:underline inline-block shrink-0"
                                title="Xem bài viết gốc"
                              >
                                <ExternalLink className="h-3 w-3" />
                              </a>
                            )}
                          </div>
                        ) : (
                          <span className="italic text-muted-foreground/60">Không ghi rõ</span>
                        )}
                      </td>

                      <td className="p-3 text-center">
                        <div className="inline-flex items-center gap-0.5 text-amber-500 font-semibold text-[11px]">
                          {Array.from({ length: item.priority }).map((_, i) => (
                            <Star key={i} className="h-3 w-3 fill-amber-500 text-amber-500" />
                          ))}
                        </div>
                        <div className="text-[10px] text-muted-foreground">
                          {item.priority === 3 ? "Rất cao" : item.priority === 2 ? "Khuyến nghị" : "Thường"}
                        </div>
                      </td>

                      <td className="p-3 text-center">
                        <button
                          onClick={() => handleToggleActive(item.id)}
                          className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-[10px] font-semibold transition-all cursor-pointer ${
                            item.isActive
                              ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/25"
                              : "bg-muted text-muted-foreground hover:bg-muted/80"
                          }`}
                          title="Click để bật/tắt sử dụng cho AI"
                        >
                          {item.isActive ? (
                            <>
                              <CheckCircle2 className="h-3 w-3" /> Bật
                            </>
                          ) : (
                            <>
                              <XCircle className="h-3 w-3" /> Tắt
                            </>
                          )}
                        </button>
                      </td>

                      <td className="p-3 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            size="icon"
                            variant="ghost"
                            onClick={() => handleOpenEdit(item)}
                            className="h-7 w-7 text-muted-foreground hover:text-foreground"
                            title="Chỉnh sửa tri thức"
                          >
                            <Pencil className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            size="icon"
                            variant="ghost"
                            onClick={() => handleDelete(item.id, item.title)}
                            className="h-7 w-7 text-destructive hover:bg-destructive/10"
                            title="Xóa tri thức"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalElements > pageSize && (
          <div className="p-3 border-t bg-muted/20 flex items-center justify-between text-xs">
            <span className="text-muted-foreground">
              Hiển thị trang {page + 1} / {Math.ceil(totalElements / pageSize)}
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
                disabled={(page + 1) * pageSize >= totalElements}
                onClick={() => setPage((p) => p + 1)}
                className="h-7 text-xs px-2.5"
              >
                Sau
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* MODAL: Thêm / Sửa Tri thức */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
          <Card className="w-full max-w-lg border-border/80 shadow-2xl bg-card overflow-hidden max-h-[90vh] flex flex-col">
            <div className="p-4 border-b flex items-center justify-between bg-muted/40 shrink-0">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-primary/10 text-primary">
                  <Sparkles className="h-4 w-4" />
                </div>
                <h3 className="font-bold text-sm">
                  {editingItem ? "Chỉnh sửa Tri Thức Du Lịch" : "Thêm Tri Thức Mới Vào Dataset"}
                </h3>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsModalOpen(false)}
                className="rounded-full h-7 w-7"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>

            <form onSubmit={handleSave} className="p-5 space-y-3.5 text-xs overflow-y-auto flex-1">
              {/* Điểm đến & Chuyên mục */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="font-semibold text-foreground flex items-center gap-1">
                    <MapPin className="h-3 w-3 text-primary" /> Điểm đến *
                  </label>
                  <Input
                    required
                    placeholder="VD: Da Nang, Hanoi, Phu Quoc..."
                    value={formDestName}
                    onChange={(e) => setFormDestName(e.target.value)}
                    className="h-8 text-xs bg-background"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-semibold text-foreground">Chuyên mục *</label>
                  <select
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value as KnowledgeCategory)}
                    className="w-full h-8 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"
                  >
                    {Object.entries(CATEGORY_MAP).map(([key, value]) => (
                      <option key={key} value={key}>
                        {value.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Tiêu đề */}
              <div className="space-y-1">
                <label className="font-semibold text-foreground">Tiêu đề mẹo / kinh nghiệm *</label>
                <Input
                  required
                  placeholder="VD: Kinh nghiệm ăn Bánh xèo Bà Dưỡng tránh giờ đông đúc"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  className="h-8 text-xs bg-background"
                />
              </div>

              {/* Nội dung chi tiết */}
              <div className="space-y-1">
                <label className="font-semibold text-foreground">
                  Nội dung chi tiết (Mẹo thực tế, giờ giấc, cách di chuyển, lưu ý...) *
                </label>
                <textarea
                  required
                  rows={4}
                  placeholder="Nhập nội dung chia sẻ thực tế được thu thập từ cộng đồng hoặc review..."
                  value={formContent}
                  onChange={(e) => setFormContent(e.target.value)}
                  className="w-full rounded-lg border border-border bg-background p-2.5 text-xs outline-none focus:ring-1 focus:ring-primary resize-none leading-relaxed"
                />
              </div>

              {/* Địa điểm cụ thể (nếu có) */}
              <div className="space-y-1">
                <label className="font-semibold text-foreground">Địa điểm nhắc tới (tùy chọn)</label>
                <Input
                  placeholder="VD: Quán Năm Đảnh, Cầu Rồng, Bà Nà Hills..."
                  value={formPlaceName}
                  onChange={(e) => setFormPlaceName(e.target.value)}
                  className="h-8 text-xs bg-background"
                />
              </div>

              {/* Nguồn & Link nguồn */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="font-semibold text-foreground">Nguồn tham khảo</label>
                  <Input
                    placeholder="VD: Group Review Đà Nẵng, TikTok @haotravel..."
                    value={formSourceName}
                    onChange={(e) => setFormSourceName(e.target.value)}
                    className="h-8 text-xs bg-background"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-semibold text-foreground">Đường dẫn bài viết (URL)</label>
                  <Input
                    placeholder="https://facebook.com/..."
                    value={formSourceUrl}
                    onChange={(e) => setFormSourceUrl(e.target.value)}
                    className="h-8 text-xs bg-background"
                  />
                </div>
              </div>

              {/* Tags & Độ ưu tiên */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="font-semibold text-foreground flex items-center gap-1">
                    <Tag className="h-3 w-3 text-muted-foreground" /> Thẻ từ khóa (phân cách bằng dấu phẩy)
                  </label>
                  <Input
                    placeholder="VD: bánh xèo, ăn vặt, buổi chiều"
                    value={formTagsStr}
                    onChange={(e) => setFormTagsStr(e.target.value)}
                    className="h-8 text-xs bg-background"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-semibold text-foreground">Độ ưu tiên cho AI *</label>
                  <select
                    value={formPriority}
                    onChange={(e) => setFormPriority(Number(e.target.value))}
                    className="w-full h-8 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"
                  >
                    <option value={1}>⭐ Mức 1 - Tham khảo bình thường</option>
                    <option value={2}>⭐⭐ Mức 2 - Khuyến nghị cao cho AI</option>
                    <option value={3}>⭐⭐⭐ Mức 3 - Đặc biệt quan trọng (Bắt buộc)</option>
                  </select>
                </div>
              </div>

              {/* Kích hoạt */}
              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="knowledgeActiveCheck"
                  checked={formIsActive}
                  onChange={(e) => setFormIsActive(e.target.checked)}
                  className="rounded border-border text-primary h-4 w-4 cursor-pointer"
                />
                <label htmlFor="knowledgeActiveCheck" className="font-medium cursor-pointer text-xs">
                  Kích hoạt để AI lập lịch trình và trả lời tư vấn
                </label>
              </div>

              {/* Actions Footer */}
              <div className="pt-3 flex justify-end gap-2 border-t shrink-0">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsModalOpen(false)}
                  disabled={isSubmitting}
                >
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={isSubmitting} className="gap-1.5">
                  <Sparkles className="h-3.5 w-3.5" />
                  {isSubmitting ? "Đang lưu..." : editingItem ? "Lưu cập nhật" : "Thêm vào dataset"}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  )
}
