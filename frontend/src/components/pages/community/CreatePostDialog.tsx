import { blogApi } from "@/client/api/blog-api"
import { getErrorMessage } from "@/client/errors"
import type { BlogPost } from "@/client/types"
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
import { useTrips } from "@/hooks/use-trips"
import { MAX_BLOG_PHOTOS, validatePhotoUrls } from "@/lib/validation"
import { useMutation } from "@tanstack/react-query"
import { Compass, Image as ImageIcon, Loader2, MapPin, Plus, Send, Sparkles } from "lucide-react"
import { useEffect, useState } from "react"
import { toast } from "sonner"

export interface InitialPostData {
  title?: string
  destinationName?: string
  content?: string
  tripId?: number
  photoUrls?: string[]
}

interface CreatePostDialogProps {
  isOpen: boolean
  onClose: () => void
  onCreated: (post: BlogPost) => void
  initialData?: InitialPostData | null
}

export function CreatePostDialog({ isOpen, onClose, onCreated, initialData }: CreatePostDialogProps) {
  const [destination, setDestination] = useState("")
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [photos, setPhotos] = useState("")
  const [selectedTripId, setSelectedTripId] = useState<number | undefined>(undefined)

  const { data: myTrips = [] } = useTrips()

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        setDestination(initialData.destinationName || "")
        setTitle(initialData.title || "")
        setContent(initialData.content || "")
        setSelectedTripId(initialData.tripId)
        setPhotos(initialData.photoUrls?.join("\n") || "")
      } else {
        setDestination("")
        setTitle("")
        setContent("")
        setSelectedTripId(undefined)
        setPhotos("")
      }
    }
  }, [isOpen, initialData])

  const createMutation = useMutation({
    mutationFn: blogApi.createPost,
    onSuccess: (created) => {
      setDestination("")
      setTitle("")
      setContent("")
      setPhotos("")
      setSelectedTripId(undefined)
      toast.success("Đăng bài chia sẻ thành công!")
      onCreated(created)
      onClose()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể đăng bài viết")),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!destination.trim() || !title.trim() || !content.trim()) {
      toast.error("Vui lòng nhập đầy đủ điểm đến, tiêu đề và nội dung bài viết!")
      return
    }
    const photoUrls = photos
      .split("\n")
      .map((p) => p.trim())
      .filter((p) => p.length > 0)
    const photoError = validatePhotoUrls(photoUrls)
    if (photoError) {
      toast.error(photoError)
      return
    }
    createMutation.mutate({
      destinationName: destination.trim(),
      title: title.trim(),
      content: content.trim(),
      tripId: selectedTripId,
      photoUrls,
    })
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && !createMutation.isPending && onClose()}>
      <DialogContent>
        <DialogHeader>
          <div className="p-1.5 rounded-lg bg-primary/10 text-primary">
            <Plus className="h-4 w-4" aria-hidden />
          </div>
          <div>
            <DialogTitle className="text-sm">Viết bài chia sẻ kinh nghiệm</DialogTitle>
            <DialogDescription>Bài viết hữu ích có thể được AI đúc kết thành tri thức</DialogDescription>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-col min-h-0">
          <DialogBody className="space-y-3.5 text-xs">
            <div className="space-y-1">
              <label htmlFor="post-destination" className="font-semibold text-foreground flex items-center gap-1">
                <MapPin className="h-3 w-3 text-primary" aria-hidden /> Điểm đến / Thành phố *
              </label>
              <Input
                id="post-destination"
                required
                maxLength={160}
                placeholder="VD: Da Nang, Hanoi, Phu Quoc, Hoi An..."
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                className="h-8.5 text-xs bg-background"
              />
            </div>

            <div className="space-y-1">
              <label htmlFor="post-title" className="font-semibold text-foreground">
                Tiêu đề bài viết *
              </label>
              <Input
                id="post-title"
                required
                maxLength={255}
                placeholder="VD: Top 5 quán bánh xèo chuẩn vị người bản địa Đà Nẵng..."
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="h-8.5 text-xs bg-background"
              />
            </div>

            <div className="space-y-1">
              <label htmlFor="post-content" className="font-semibold text-foreground">
                Nội dung đánh giá &amp; kinh nghiệm thực tế *
              </label>
              <textarea
                id="post-content"
                required
                rows={6}
                placeholder="Chia sẻ chi tiết: Tên quán/địa điểm, giá cả, thời gian nên đi, mẹo tránh xếp hàng hoặc lưu ý cho chuyến đi..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="w-full rounded-lg border border-border bg-background p-2.5 text-xs outline-none focus:ring-1 focus:ring-primary resize-none leading-relaxed"
              />
            </div>

            {initialData && (
              <div className="p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center gap-2 text-xs text-amber-700 dark:text-amber-300">
                <Sparkles className="h-4 w-4 shrink-0 text-amber-500 animate-pulse" />
                <span>
                  Nội dung đã được Trợ lý AI tổng hợp tự động từ chuyến đi thực tế. Bạn có thể kiểm tra và chỉnh sửa
                  trước khi đăng!
                </span>
              </div>
            )}

            <div className="space-y-1">
              <label htmlFor="post-trip" className="font-semibold text-foreground flex items-center gap-1">
                <Compass className="h-3 w-3 text-primary" aria-hidden /> Gắn kèm lịch trình chuyến đi (Tùy chọn)
              </label>
              <select
                id="post-trip"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none focus:ring-1 focus:ring-primary cursor-pointer"
                value={selectedTripId ?? ""}
                onChange={(e) => {
                  const val = e.target.value ? Number(e.target.value) : undefined
                  setSelectedTripId(val)
                  if (val) {
                    const found = myTrips.find((t) => t.id === val)
                    if (found && !destination) {
                      setDestination(found.destination?.name || "")
                    }
                  }
                }}
              >
                <option value="">-- Không gắn kèm lịch trình --</option>
                {myTrips.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name} ({t.destination?.name || "Điểm đến"})
                  </option>
                ))}
              </select>
              <p className="text-[11px] text-muted-foreground">
                Khi gắn kèm, thành viên khác trên Cộng đồng có thể bấm &quot;Sao chép lịch trình&quot; này về chuyến đi
                của họ.
              </p>
            </div>

            <div className="space-y-1">
              <label htmlFor="post-photos" className="font-semibold text-foreground flex items-center gap-1">
                <ImageIcon className="h-3 w-3 text-muted-foreground" aria-hidden /> Link hình ảnh https (mỗi dòng 1
                link, tối đa {MAX_BLOG_PHOTOS}, tùy chọn)
              </label>
              <textarea
                id="post-photos"
                rows={2}
                placeholder="https://images.unsplash.com/..."
                value={photos}
                onChange={(e) => setPhotos(e.target.value)}
                className="w-full rounded-lg border border-border bg-background p-2 text-xs outline-none focus:ring-1 focus:ring-primary resize-none font-mono"
              />
            </div>
          </DialogBody>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onClose}
              disabled={createMutation.isPending}
              className="text-xs"
            >
              Hủy
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={createMutation.isPending}
              className="text-xs font-semibold gap-1.5"
            >
              {createMutation.isPending ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" /> Đang đăng...
                </>
              ) : (
                <>
                  <Send className="h-3.5 w-3.5" /> Đăng bài
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
