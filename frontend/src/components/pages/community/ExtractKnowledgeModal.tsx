import { useState, useEffect } from "react"
import { BlogPost, KnowledgeCategory, KnowledgeRequest, ExtractedKnowledge } from "@/client/types"
import { blogApi } from "@/client/api/blog-api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card } from "@/components/ui/card"
import {
  Sparkles,
  X,
  MapPin,
  Tag,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Lightbulb,
  Database,
} from "lucide-react"
import { toast } from "sonner"

interface ExtractKnowledgeModalProps {
  post: BlogPost
  isOpen: boolean
  onClose: () => void
  onSuccess: (convertedKnowledgeId: number) => void
}

const CATEGORY_MAP: Record<KnowledgeCategory, { label: string; color: string }> = {
  FOOD: { label: "Ẩm thực & Quán xá", color: "text-amber-600 bg-amber-500/10 border-amber-500/30" },
  SIGHTSEEING: { label: "Tham quan & Check-in", color: "text-blue-600 bg-blue-500/10 border-blue-500/30" },
  ACCOMMODATION: { label: "Lưu trú & Khách sạn", color: "text-purple-600 bg-purple-500/10 border-purple-500/30" },
  TRANSPORT: { label: "Di chuyển & Phương tiện", color: "text-emerald-600 bg-emerald-500/10 border-emerald-500/30" },
  TIP: { label: "Mẹo & Kinh nghiệm thực tế", color: "text-indigo-600 bg-indigo-500/10 border-indigo-500/30" },
  WARNING: { label: "Cảnh báo & Lưu ý tránh bẫy", color: "text-rose-600 bg-rose-500/10 border-rose-500/30" },
}

export function ExtractKnowledgeModal({
  post,
  isOpen,
  onClose,
  onSuccess,
}: ExtractKnowledgeModalProps) {
  const [loading, setLoading] = useState<boolean>(true)
  const [submitting, setSubmitting] = useState<boolean>(false)
  const [extractedData, setExtractedData] = useState<ExtractedKnowledge | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Form states
  const [formDestName, setFormDestName] = useState<string>(post.destinationName)
  const [formCategory, setFormCategory] = useState<KnowledgeCategory>("TIP")
  const [formTitle, setFormTitle] = useState<string>("")
  const [formContent, setFormContent] = useState<string>("")
  const [formPlaceName, setFormPlaceName] = useState<string>("")
  const [formSourceName, setFormSourceName] = useState<string>(`Cộng đồng TripMind - ${post.user.name}`)
  const [formSourceUrl, setFormSourceUrl] = useState<string>(window.location.origin + `/community?post=${post.id}`)
  const [formTagsStr, setFormTagsStr] = useState<string>("")
  const [formPriority, setFormPriority] = useState<number>(2)
  const [formActive, setFormActive] = useState<boolean>(true)

  useEffect(() => {
    if (!isOpen) return

    let isMounted = true
    setLoading(true)
    setError(null)

    blogApi
      .extractKnowledge(post.id)
      .then((res) => {
        if (!isMounted) return
        setExtractedData(res)
        setFormDestName(res.destinationName || post.destinationName)
        setFormCategory(res.category || "TIP")
        setFormTitle(res.title || post.title)
        setFormContent(res.content || "")
        setFormPlaceName(res.placeName || "")
        setFormTagsStr(res.tags ? res.tags.join(", ") : "")
        setFormPriority(res.priority || 2)
        setFormActive(res.active ?? true)
        setLoading(false)
      })
      .catch((err) => {
        if (!isMounted) return
        console.error("AI extraction error:", err)
        setError(err.message || "Không thể trích xuất tri thức bằng AI. Bạn có thể tự điền nội dung.")
        // Fallback default values
        setFormDestName(post.destinationName)
        setFormTitle(post.title)
        setFormContent(post.content)
        setLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [isOpen, post.id, post.destinationName, post.title, post.content, post.user.name])

  if (!isOpen) return null

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formDestName.trim() || !formTitle.trim() || !formContent.trim()) {
      toast.error("Vui lòng điền đủ điểm đến, tiêu đề và nội dung tri thức!")
      return
    }

    setSubmitting(true)
    try {
      const tags = formTagsStr
        .split(",")
        .map((t) => t.trim())
        .filter((t) => t.length > 0)

      const request: KnowledgeRequest = {
        destinationName: formDestName.trim(),
        category: formCategory,
        title: formTitle.trim(),
        content: formContent.trim(),
        placeName: formPlaceName.trim() ? formPlaceName.trim() : undefined,
        sourceName: formSourceName.trim() ? formSourceName.trim() : undefined,
        sourceUrl: formSourceUrl.trim() ? formSourceUrl.trim() : undefined,
        tags,
        priority: formPriority,
        active: formActive,
      }

      const saved = await blogApi.convertToKnowledge(post.id, request)
      toast.success("Đã thêm thành công vào Dataset Tri Thức AI!")
      onSuccess(saved.id)
      onClose()
    } catch (err: any) {
      console.error("Lỗi khi lưu tri thức:", err)
      toast.error(err.message || "Không thể lưu tri thức vào hệ thống")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <Card className="w-full max-w-2xl border-border/80 shadow-2xl bg-card overflow-hidden max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="p-4 border-b flex items-center justify-between bg-gradient-to-r from-amber-500/10 via-primary/10 to-transparent shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-600 dark:text-amber-400 border border-amber-500/30">
              <Sparkles className="h-5 w-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-sm sm:text-base text-foreground">
                  Trích xuất Dataset Tri Thức Cho AI
                </h3>
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-700 dark:text-amber-300 border border-amber-500/30">
                  Admin Exclusive
                </span>
              </div>
              <p className="text-[11px] text-muted-foreground line-clamp-1 mt-0.5">
                Bài viết: &quot;{post.title}&quot; &bull; {post.user.name} ({post.commentCount} bình luận)
              </p>
            </div>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={onClose}
            className="rounded-full h-8 w-8 hover:bg-muted"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Content Body */}
        {loading ? (
          <div className="p-12 flex flex-col items-center justify-center text-center space-y-4">
            <div className="relative">
              <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center animate-pulse">
                <Database className="h-8 w-8 text-amber-500" />
              </div>
              <Loader2 className="h-6 w-6 text-primary animate-spin absolute -top-2 -right-2" />
            </div>
            <div className="space-y-1.5 max-w-sm">
              <h4 className="font-semibold text-sm">AI đang tổng hợp & tinh gọn tri thức</h4>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Đang đọc bài viết và {post.commentCount} bình luận của cộng đồng để lọc ra các kinh nghiệm, mẹo thực tế, địa điểm chất lượng cao...
              </p>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSave} className="p-5 space-y-4 text-xs overflow-y-auto flex-1">
            {error && (
              <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs flex items-center gap-2">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {extractedData?.reasoning && (
              <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/25 space-y-1">
                <div className="flex items-center gap-1.5 font-bold text-amber-700 dark:text-amber-300 text-xs">
                  <Lightbulb className="h-3.5 w-3.5" />
                  <span>Phân tích & Đánh giá của AI</span>
                </div>
                <p className="text-[11px] text-muted-foreground leading-relaxed">
                  {extractedData.reasoning}
                </p>
              </div>
            )}

            {/* Destination & Category */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              <div className="space-y-1">
                <label className="font-semibold text-foreground flex items-center gap-1">
                  <MapPin className="h-3.5 w-3.5 text-primary" /> Điểm đến *
                </label>
                <Input
                  required
                  placeholder="VD: Da Nang, Hanoi, Phu Quoc..."
                  value={formDestName}
                  onChange={(e) => setFormDestName(e.target.value)}
                  className="h-8.5 text-xs bg-background"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-foreground">Chuyên mục tri thức *</label>
                <select
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value as KnowledgeCategory)}
                  className="w-full h-8.5 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"
                >
                  {Object.entries(CATEGORY_MAP).map(([key, value]) => (
                    <option key={key} value={key}>
                      {value.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Title */}
            <div className="space-y-1">
              <label className="font-semibold text-foreground">Tiêu đề tri thức / mẹo *</label>
              <Input
                required
                placeholder="VD: Kinh nghiệm ăn Bánh xèo Bà Dưỡng chuẩn vị tránh hàng nhái"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                className="h-8.5 text-xs bg-background"
              />
            </div>

            {/* Specific Place Name */}
            <div className="space-y-1">
              <label className="font-semibold text-foreground flex items-center justify-between">
                <span>Địa điểm cụ thể được đề cập (Tùy chọn)</span>
                <span className="text-[10px] text-muted-foreground font-normal">
                  Nếu có, AI sẽ ưu tiên đưa vào lịch trình
                </span>
              </label>
              <Input
                placeholder="VD: Quán Bánh Xèo Bà Dưỡng, Hải Sản Năm Đảnh..."
                value={formPlaceName}
                onChange={(e) => setFormPlaceName(e.target.value)}
                className="h-8.5 text-xs bg-background"
              />
            </div>

            {/* Distilled Content */}
            <div className="space-y-1">
              <label className="font-semibold text-foreground flex items-center justify-between">
                <span>Nội dung đã được AI chắt lọc & đúc kết *</span>
                <span className="text-[10px] text-primary font-medium">
                  {formContent.length} ký tự
                </span>
              </label>
              <textarea
                required
                rows={4}
                placeholder="Nội dung tri thức tinh gọn, chứa mẹo, kinh nghiệm, thời điểm lý tưởng, lưu ý..."
                value={formContent}
                onChange={(e) => setFormContent(e.target.value)}
                className="w-full rounded-lg border border-border bg-background p-2.5 text-xs outline-none focus:ring-1 focus:ring-primary resize-none leading-relaxed font-sans"
              />
            </div>

            {/* Tags & Priority */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              <div className="space-y-1">
                <label className="font-semibold text-foreground flex items-center gap-1">
                  <Tag className="h-3 w-3 text-muted-foreground" /> Thẻ từ khóa (phân cách bằng dấu phẩy)
                </label>
                <Input
                  placeholder="ẩm thực, đặc sản, ăn tối, giá rẻ"
                  value={formTagsStr}
                  onChange={(e) => setFormTagsStr(e.target.value)}
                  className="h-8.5 text-xs bg-background"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-foreground">Độ ưu tiên cho AI gợi ý *</label>
                <select
                  value={formPriority}
                  onChange={(e) => setFormPriority(Number(e.target.value))}
                  className="w-full h-8.5 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"
                >
                  <option value={1}>⭐ Mức 1 - Tham khảo bổ trợ</option>
                  <option value={2}>⭐⭐ Mức 2 - Ưu tiên đề xuất (Mặc định)</option>
                  <option value={3}>⭐⭐⭐ Mức 3 - Đặc biệt quan trọng</option>
                </select>
              </div>
            </div>

            {/* Source info */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 text-muted-foreground">
              <div className="space-y-1">
                <label className="font-medium text-[11px]">Nguồn tham khảo</label>
                <Input
                  value={formSourceName}
                  onChange={(e) => setFormSourceName(e.target.value)}
                  className="h-8 text-xs bg-background/50"
                />
              </div>
              <div className="space-y-1">
                <label className="font-medium text-[11px]">Link gốc</label>
                <Input
                  value={formSourceUrl}
                  onChange={(e) => setFormSourceUrl(e.target.value)}
                  className="h-8 text-xs bg-background/50"
                />
              </div>
            </div>

            {/* Action buttons in footer */}
            <div className="pt-3 border-t flex items-center justify-between">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={formActive}
                  onChange={(e) => setFormActive(e.target.checked)}
                  className="h-4 w-4 rounded border-border text-primary focus:ring-primary cursor-pointer"
                />
                <span className="font-semibold text-xs text-foreground">
                  Kích hoạt ngay cho AI sử dụng
                </span>
              </label>

              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={onClose}
                  disabled={submitting}
                  className="text-xs"
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={submitting}
                  className="gap-1.5 text-xs font-semibold bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-600 hover:to-amber-700 text-white shadow-sm"
                >
                  {submitting ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 animate-spin" /> Đang lưu...
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="h-3.5 w-3.5" /> Xác nhận lưu vào Dataset
                    </>
                  )}
                </Button>
              </div>
            </div>
          </form>
        )}
      </Card>
    </div>
  )
}
