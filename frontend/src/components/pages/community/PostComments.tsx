import { blogApi } from "@/client/api/blog-api"
import { getErrorMessage } from "@/client/errors"
import type { BlogComment } from "@/client/types"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { queryKeys } from "@/hooks/query-keys"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Heart, Loader2, MessageSquare, Send } from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"
import { formatTimeAgo, updateCachedPosts } from "./blog-cache"

interface PostCommentsProps {
  postId: number
  canInteract: boolean
}

export function PostComments({ postId, canInteract }: PostCommentsProps) {
  const queryClient = useQueryClient()
  const key = queryKeys.blogComments(postId)
  const [text, setText] = useState("")

  const {
    data: comments = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: key,
    queryFn: () => blogApi.getComments(postId),
  })

  const addMutation = useMutation({
    mutationFn: (content: string) => blogApi.addComment(postId, content),
    onSuccess: (created) => {
      setText("")
      queryClient.setQueryData<BlogComment[]>(key, (prev) => [...(prev ?? []), created])
      updateCachedPosts(queryClient, (p) => (p.id === postId ? { ...p, commentCount: p.commentCount + 1 } : p))
      toast.success("Đã gửi bình luận")
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể gửi bình luận")),
  })

  const upvoteMutation = useMutation({
    mutationFn: (commentId: number) => blogApi.toggleCommentUpvote(commentId),
    onSuccess: (updated) =>
      queryClient.setQueryData<BlogComment[]>(key, (prev) =>
        prev?.map((c) =>
          c.id === updated.id ? { ...c, upvoteCount: updated.upvoteCount, isUpvoted: updated.isUpvoted } : c,
        ),
      ),
    onError: (err) => toast.error(getErrorMessage(err, "Không thể bình chọn bình luận")),
  })

  const submit = () => {
    if (!canInteract) {
      toast.error("Vui lòng đăng nhập để bình luận!")
      return
    }
    const content = text.trim()
    if (content && !addMutation.isPending) addMutation.mutate(content)
  }

  return (
    <div className="pt-4 border-t space-y-3 bg-muted/20 -mx-5 -mb-5 sm:-mx-6 sm:-mb-6 p-4 sm:p-5 rounded-b-xl">
      <h4 className="text-xs font-bold text-foreground flex items-center gap-1.5">
        <MessageSquare className="h-3.5 w-3.5 text-primary" aria-hidden />
        <span>Thảo luận cộng đồng ({comments.length})</span>
      </h4>

      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
      >
        <Input
          aria-label="Nội dung bình luận"
          placeholder="Viết cảm nghĩ hoặc chia sẻ thêm mẹo của bạn..."
          value={text}
          onChange={(e) => setText(e.target.value)}
          className="h-8.5 text-xs bg-background"
        />
        <Button
          type="submit"
          size="sm"
          disabled={!text.trim() || addMutation.isPending}
          className="h-8.5 gap-1 text-xs font-semibold shrink-0"
        >
          <Send className="h-3 w-3" /> Gửi
        </Button>
      </form>

      {isLoading ? (
        <div className="py-4 text-center text-xs text-muted-foreground flex items-center justify-center gap-2">
          <Loader2 className="h-3.5 w-3.5 animate-spin text-primary" aria-hidden />
          <span>Đang tải bình luận...</span>
        </div>
      ) : isError ? (
        <p className="text-xs text-muted-foreground text-center py-2">
          {getErrorMessage(error, "Không thể tải bình luận")}
        </p>
      ) : comments.length === 0 ? (
        <p className="text-xs text-muted-foreground text-center py-2 italic">
          Chưa có bình luận nào. Hãy bắt đầu cuộc trò chuyện!
        </p>
      ) : (
        <ul className="space-y-2.5 pt-1">
          {comments.map((comment) => (
            <li key={comment.id} className="p-3 rounded-xl bg-background border border-border/80 text-xs space-y-1.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-foreground">{comment.user.name}</span>
                  <span className="text-[10px] text-muted-foreground">&bull; {formatTimeAgo(comment.createdAt)}</span>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  aria-pressed={comment.isUpvoted}
                  aria-label={`Thích bình luận của ${comment.user.name}`}
                  disabled={upvoteMutation.isPending}
                  onClick={() =>
                    canInteract
                      ? upvoteMutation.mutate(comment.id)
                      : toast.error("Vui lòng đăng nhập để thích bình luận!")
                  }
                  className={`h-6 px-1.5 text-[11px] gap-1 ${
                    comment.isUpvoted ? "text-rose-500 font-semibold" : "text-muted-foreground hover:text-rose-500"
                  }`}
                >
                  <Heart className={`h-3 w-3 ${comment.isUpvoted ? "fill-current" : ""}`} />
                  <span>{comment.upvoteCount}</span>
                </Button>
              </div>
              <p className="text-muted-foreground leading-relaxed whitespace-pre-line text-xs">{comment.content}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
