import type { BlogPost } from "@/client/types"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import {
  Calendar,
  ChevronDown,
  ChevronUp,
  Copy,
  Heart,
  MapPin,
  MessageSquare,
  Share2,
  Sparkles,
  Trash2,
} from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"
import { PostComments } from "./PostComments"
import { formatTimeAgo } from "./blog-cache"

interface PostCardProps {
  post: BlogPost
  canInteract: boolean
  canDelete: boolean
  isAdmin: boolean
  upvoting: boolean
  onUpvote: (post: BlogPost) => void
  onDelete: (post: BlogPost) => void
  onExtract: (post: BlogPost) => void
  onCloneTrip?: (post: BlogPost) => void
}

export function PostCard({
  post,
  canInteract,
  canDelete,
  isAdmin,
  upvoting,
  onUpvote,
  onDelete,
  onExtract,
  onCloneTrip,
}: PostCardProps) {
  const [commentsOpen, setCommentsOpen] = useState(false)
  const commentsId = `post-${post.id}-comments`

  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}/community?post=${post.id}`)
      toast.success("Đã sao chép liên kết bài viết!")
    } catch {
      toast.error("Không thể sao chép liên kết")
    }
  }

  return (
    <Card className="overflow-hidden border-border/80 shadow-sm hover:shadow-md transition-all duration-200 bg-card">
      <article className="p-5 sm:p-6 space-y-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            {post.user.avatarUrl ? (
              <img
                src={post.user.avatarUrl}
                alt=""
                className="w-10 h-10 rounded-full object-cover border border-border"
              />
            ) : (
              <div
                aria-hidden
                className="w-10 h-10 rounded-full bg-gradient-to-tr from-primary/30 to-amber-500/30 flex items-center justify-center font-bold text-sm text-foreground border border-border"
              >
                {post.user.name ? post.user.name.charAt(0).toUpperCase() : "U"}
              </div>
            )}
            <div>
              <span className="font-bold text-sm text-foreground">{post.user.name}</span>
              <div className="flex items-center gap-2 text-xs text-muted-foreground mt-0.5">
                <span className="flex items-center gap-1">
                  <Calendar className="h-3 w-3" aria-hidden /> {formatTimeAgo(post.createdAt)}
                </span>
                <span>&bull;</span>
                <span className="flex items-center gap-1 font-medium text-primary">
                  <MapPin className="h-3 w-3" aria-hidden /> {post.destinationName}
                </span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {post.isConvertedToKnowledge && (
              <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30">
                <Sparkles className="h-3 w-3 text-amber-500" aria-hidden />
                <span>Đã lưu vào Tri Thức AI</span>
              </div>
            )}
            {canDelete && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => onDelete(post)}
                className="h-7 w-7 text-muted-foreground hover:text-rose-500"
                title="Xóa bài viết"
                aria-label={`Xóa bài viết ${post.title}`}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>
        </div>

        <div className="space-y-2">
          <h2 className="text-base sm:text-lg font-bold text-foreground">{post.title}</h2>
          <p className="text-xs sm:text-sm text-muted-foreground whitespace-pre-line leading-relaxed">{post.content}</p>
        </div>

        {post.photoUrls && post.photoUrls.length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pt-1">
            {post.photoUrls.map((url) => (
              <a
                key={url}
                href={url}
                target="_blank"
                rel="noreferrer"
                className="relative aspect-video rounded-xl overflow-hidden bg-muted border border-border group block"
              >
                <img
                  src={url}
                  alt={`Ảnh bài viết ${post.title}`}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  onError={(e) => {
                    e.currentTarget.style.display = "none"
                  }}
                />
              </a>
            ))}
          </div>
        )}

        <div className="pt-3 border-t flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Button
              variant={post.isUpvoted ? "default" : "outline"}
              size="sm"
              aria-pressed={post.isUpvoted}
              disabled={upvoting}
              onClick={() => (canInteract ? onUpvote(post) : toast.error("Vui lòng đăng nhập để bình chọn bài viết!"))}
              className={`h-8 gap-1.5 text-xs font-semibold transition-all ${
                post.isUpvoted
                  ? "bg-rose-500 hover:bg-rose-600 text-white border-rose-500 shadow-sm"
                  : "hover:text-rose-500 hover:border-rose-500/50"
              }`}
            >
              <Heart className={`h-3.5 w-3.5 ${post.isUpvoted ? "fill-current" : ""}`} />
              <span>Hữu ích ({post.upvoteCount})</span>
            </Button>

            {isAdmin && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onExtract(post)}
                className={`h-8 gap-1.5 text-xs font-semibold transition-all ${
                  post.isConvertedToKnowledge
                    ? "border-amber-500/40 text-amber-700 dark:text-amber-300 bg-amber-500/10 hover:bg-amber-500/20"
                    : "border-amber-500/60 bg-gradient-to-r from-amber-500/15 to-orange-500/15 text-amber-700 dark:text-amber-300 hover:from-amber-500/25 hover:to-orange-500/25 shadow-xs"
                }`}
                title="Trích xuất bài viết và thảo luận thành tri thức Dataset cho AI"
              >
                <Sparkles className="h-3.5 w-3.5 text-amber-500 animate-pulse" />
                <span>{post.isConvertedToKnowledge ? "Cập nhật Dataset AI" : "⚡ Trích xuất Dataset AI"}</span>
              </Button>
            )}

            <Button
              variant="ghost"
              size="sm"
              aria-expanded={commentsOpen}
              aria-controls={commentsId}
              onClick={() => setCommentsOpen((v) => !v)}
              className="h-8 gap-1.5 text-xs text-muted-foreground hover:text-foreground"
            >
              <MessageSquare className="h-3.5 w-3.5" />
              <span>Bình luận ({post.commentCount})</span>
              {commentsOpen ? <ChevronUp className="h-3 w-3 ml-0.5" /> : <ChevronDown className="h-3 w-3 ml-0.5" />}
            </Button>

            {post.tripId && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onCloneTrip?.(post)}
                className="h-8 gap-1.5 text-xs font-semibold text-primary border-primary/30 bg-primary/5 hover:bg-primary/10 shadow-xs cursor-pointer"
                title="Sao chép khung lịch trình chuyến đi này về tài khoản của bạn"
              >
                <Copy className="h-3.5 w-3.5" />
                <span>Sao chép lịch trình</span>
              </Button>
            )}
          </div>

          <Button
            variant="ghost"
            size="sm"
            onClick={handleShare}
            aria-label="Chia sẻ bài viết"
            className="h-8 gap-1 text-xs text-muted-foreground hover:text-foreground"
          >
            <Share2 className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">Chia sẻ</span>
          </Button>
        </div>

        {commentsOpen && (
          <div id={commentsId}>
            <PostComments postId={post.id} canInteract={canInteract} />
          </div>
        )}
      </article>
    </Card>
  )
}
