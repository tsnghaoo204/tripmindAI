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
import { MAX_BLOG_PHOTOS, validatePhotoUrls } from "@/lib/validation"
import { useMutation } from "@tanstack/react-query"
import { Image as ImageIcon, Loader2, MapPin, Plus, Send } from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"

interface CreatePostDialogProps {
  isOpen: boolean
  onClose: () => void
  onCreated: (post: BlogPost) => void
}

export function CreatePostDialog({ isOpen, onClose, onCreated }: CreatePostDialogProps) {
  const [destination, setDestination] = useState("")
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [photos, setPhotos] = useState("")

  const createMutation = useMutation({
    mutationFn: blogApi.createPost,
    onSuccess: (created) => {
      setDestination("")
      setTitle("")
      setContent("")
      setPhotos("")
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
