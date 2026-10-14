import { adminApi } from "@/client/api/admin-api"
import { getErrorMessage } from "@/client/errors"
import type { DestinationKnowledge, KnowledgeCategory, KnowledgeRequest } from "@/client/types"
import { Button } from "@/components/ui/button"
import { Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { MapPin, Sparkles, Tag } from "lucide-react"
import { useEffect, useState } from "react"
import { toast } from "sonner"
import { KNOWLEDGE_CATEGORIES, KNOWLEDGE_CATEGORY_IDS } from "./knowledge-categories"

const SELECT_CLASS =
  "w-full h-8 rounded-lg border border-border bg-background px-2.5 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer font-medium"

interface KnowledgeFormDialogProps {
  isOpen: boolean
  /** Item being edited; null creates a new one. */
  item: DestinationKnowledge | null
  defaultDestination: string
  onClose: () => void
}

export function KnowledgeFormDialog({ isOpen, item, defaultDestination, onClose }: KnowledgeFormDialogProps) {
  const queryClient = useQueryClient()
  const [destName, setDestName] = useState("")
  const [category, setCategory] = useState<KnowledgeCategory>("FOOD")
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [placeName, setPlaceName] = useState("")
  const [sourceName, setSourceName] = useState("")
  const [sourceUrl, setSourceUrl] = useState("")
  const [tags, setTags] = useState("")
  const [priority, setPriority] = useState(2)
  const [active, setActive] = useState(true)

  // Reset the form each time the dialog opens
  useEffect(() => {
    if (!isOpen) return
    setDestName(item?.destinationName ?? defaultDestination)
    setCategory(item?.category ?? "FOOD")
    setTitle(item?.title ?? "")
    setContent(item?.content ?? "")
    setPlaceName(item?.placeName ?? "")
    setSourceName(item ? (item.sourceName ?? "") : "Facebook Review Du Lịch")
    setSourceUrl(item?.sourceUrl ?? "")
    setTags(item?.tags?.join(", ") ?? "")
    setPriority(item?.priority ?? 2)
    setActive(item?.isActive ?? true)
  }, [isOpen, item, defaultDestination])

  const saveMutation = useMutation({
    mutationFn: (payload: KnowledgeRequest) =>
      item ? adminApi.updateKnowledge(item.id, payload) : adminApi.createKnowledge(payload),
    onSuccess: () => {
      toast.success(item ? "Đã cập nhật tri thức du lịch thành công!" : "Đã thêm tri thức du lịch mới vào dataset!")
      queryClient.invalidateQueries({ queryKey: ["admin", "knowledge"] })
      onClose()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể lưu tri thức. Vui lòng kiểm tra lại.")),
  })

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault()
    if (!destName.trim() || !title.trim() || !content.trim()) {
      toast.error("Vui lòng điền đầy đủ Điểm đến, Tiêu đề và Nội dung")
      return
    }
    saveMutation.mutate({
      destinationName: destName.trim(),
      category,
      title: title.trim(),
      content: content.trim(),
      placeName: placeName.trim() || undefined,
      sourceName: sourceName.trim() || undefined,
      sourceUrl: sourceUrl.trim() || undefined,
      tags: tags
        .split(",")
        .map((t) => t.trim())
        .filter((t) => t.length > 0),
      priority,
      active,
    })
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && !saveMutation.isPending && onClose()}>
      <DialogContent>
        <DialogHeader>
          <div className="p-1.5 rounded-lg bg-primary/10 text-primary">
            <Sparkles className="h-4 w-4" aria-hidden />
          </div>
          <DialogTitle className="text-sm">
            {item ? "Chỉnh sửa Tri Thức Du Lịch" : "Thêm Tri Thức Mới Vào Dataset"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSave} className="flex flex-col min-h-0">
          <DialogBody className="space-y-3.5 text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label htmlFor="kn-destination" className="font-semibold text-foreground flex items-center gap-1">
                  <MapPin className="h-3 w-3 text-primary" aria-hidden /> Điểm đến *
                </label>
                <Input
                  id="kn-destination"
                  required
                  maxLength={160}
                  placeholder="VD: Da Nang, Hanoi, Phu Quoc..."
                  value={destName}
                  onChange={(e) => setDestName(e.target.value)}
                  className="h-8 text-xs bg-background"
                />
              </div>
              <div className="space-y-1">
                <label htmlFor="kn-category" className="font-semibold text-foreground">
                  Chuyên mục *
                </label>
                <select
                  id="kn-category"
                  value={category}
                  onChange={(e) => setCategory(e.target.value as KnowledgeCategory)}
                  className={SELECT_CLASS}
                >
                  {KNOWLEDGE_CATEGORY_IDS.map((key) => (
                    <option key={key} value={key}>
                      {KNOWLEDGE_CATEGORIES[key].label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="space-y-1">
              <label htmlFor="kn-title" className="font-semibold text-foreground">
                Tiêu đề mẹo / kinh nghiệm *
              </label>
              <Input
                id="kn-title"
                required
                maxLength={255}
                placeholder="VD: Kinh nghiệm ăn Bánh xèo Bà Dưỡng tránh giờ đông đúc"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="h-8 text-xs bg-background"
              />
            </div>

            <div className="space-y-1">
              <label htmlFor="kn-content" className="font-semibold text-foreground">
                Nội dung chi tiết (Mẹo thực tế, giờ giấc, cách di chuyển, lưu ý...) *
              </label>
              <textarea
                id="kn-content"
                required
                rows={4}
                placeholder="Nhập nội dung chia sẻ thực tế được thu thập từ cộng đồng hoặc review..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="w-full rounded-lg border border-border bg-background p-2.5 text-xs outline-none focus:ring-1 focus:ring-primary resize-none leading-relaxed"
              />
            </div>

            <div className="space-y-1">
              <label htmlFor="kn-place" className="font-semibold text-foreground">
                Địa điểm nhắc tới (tùy chọn)
              </label>
              <Input
                id="kn-place"
                maxLength={200}
                placeholder="VD: Quán Năm Đảnh, Cầu Rồng, Bà Nà Hills..."
                value={placeName}
                onChange={(e) => setPlaceName(e.target.value)}
                className="h-8 text-xs bg-background"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label htmlFor="kn-source" className="font-semibold text-foreground">
                  Nguồn tham khảo
                </label>
                <Input
                  id="kn-source"
                  maxLength={100}
                  placeholder="VD: Group Review Đà Nẵng, TikTok @haotravel..."
                  value={sourceName}
                  onChange={(e) => setSourceName(e.target.value)}
                  className="h-8 text-xs bg-background"
                />
              </div>
              <div className="space-y-1">
                <label htmlFor="kn-source-url" className="font-semibold text-foreground">
                  Đường dẫn bài viết (URL)
                </label>
                <Input
                  id="kn-source-url"
                  type="url"
                  placeholder="https://facebook.com/..."
                  value={sourceUrl}
                  onChange={(e) => setSourceUrl(e.target.value)}
                  className="h-8 text-xs bg-background"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label htmlFor="kn-tags" className="font-semibold text-foreground flex items-center gap-1">
                  <Tag className="h-3 w-3 text-muted-foreground" aria-hidden /> Thẻ từ khóa (phân cách bằng dấu phẩy)
                </label>
                <Input
                  id="kn-tags"
                  placeholder="VD: bánh xèo, ăn vặt, buổi chiều"
                  value={tags}
                  onChange={(e) => setTags(e.target.value)}
                  className="h-8 text-xs bg-background"
                />
              </div>
              <div className="space-y-1">
                <label htmlFor="kn-priority" className="font-semibold text-foreground">
                  Độ ưu tiên cho AI *
                </label>
                <select
                  id="kn-priority"
                  value={priority}
                  onChange={(e) => setPriority(Number(e.target.value))}
                  className={SELECT_CLASS}
                >
                  <option value={1}>⭐ Mức 1 - Tham khảo bình thường</option>
                  <option value={2}>⭐⭐ Mức 2 - Khuyến nghị cao cho AI</option>
                  <option value={3}>⭐⭐⭐ Mức 3 - Đặc biệt quan trọng (Bắt buộc)</option>
                </select>
              </div>
            </div>

            <div className="flex items-center gap-2 pt-1">
              <input
                type="checkbox"
                id="kn-active"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="rounded border-border text-primary h-4 w-4 cursor-pointer"
              />
              <label htmlFor="kn-active" className="font-medium cursor-pointer text-xs">
                Kích hoạt để AI lập lịch trình và trả lời tư vấn
              </label>
            </div>
          </DialogBody>

          <DialogFooter>
            <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={saveMutation.isPending}>
              Hủy
            </Button>
            <Button type="submit" size="sm" disabled={saveMutation.isPending} className="gap-1.5">
              <Sparkles className="h-3.5 w-3.5" />
              {saveMutation.isPending ? "Đang lưu..." : item ? "Lưu cập nhật" : "Thêm vào dataset"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
