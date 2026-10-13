import { createFileRoute } from "@tanstack/react-router"
import { useState, useEffect } from "react"
import { useAuth } from "@/contexts/auth-context"
import { BlogPost, BlogComment, BlogPostRequest } from "@/client/types"
import { blogApi } from "@/client/api/blog-api"
import { ExtractKnowledgeModal } from "@/components/pages/community/ExtractKnowledgeModal"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card } from "@/components/ui/card"
import {
  Sparkles,
  Heart,
  MessageSquare,
  Plus,
  Search,
  MapPin,
  Send,
  Trash2,
  Share2,
  Calendar,
  ShieldCheck,
  ChevronDown,
  ChevronUp,
  Image as ImageIcon,
  Loader2,
  X,
  Compass,
} from "lucide-react"
import { toast } from "sonner"

export const Route = createFileRoute("/_layout/community")({
  component: CommunityPage,
})

function formatTimeAgo(dateStr: string) {
  try {
    const diffMs = Date.now() - new Date(dateStr).getTime()
    const diffMins = Math.floor(diffMs / 60000)
    if (diffMins < 1) return "Vừa xong"
    if (diffMins < 60) return `${diffMins} phút trước`
    const diffHours = Math.floor(diffMins / 60)
    if (diffHours < 24) return `${diffHours} giờ trước`
    const diffDays = Math.floor(diffHours / 24)
    if (diffDays < 30) return `${diffDays} ngày trước`
    return new Date(dateStr).toLocaleDateString("vi-VN")
  } catch {
    return dateStr
  }
}

function CommunityPage() {
  const { user } = useAuth()
  const [posts, setPosts] = useState<BlogPost[]>([])
  const [destinations, setDestinations] = useState<string[]>([])
  const [selectedDest, setSelectedDest] = useState<string>("ALL")
  const [searchQuery, setSearchQuery] = useState<string>("")
  const [loading, setLoading] = useState<boolean>(true)
  const [page, setPage] = useState<number>(0)
  const [hasMore, setHasMore] = useState<boolean>(false)

  // Comments state: map postId -> { comments: BlogComment[], loading: boolean, isOpen: boolean, newText: string }
  const [commentStates, setCommentStates] = useState<
    Record<
      number,
      {
        comments: BlogComment[]
        loading: boolean
        isOpen: boolean
        newText: string
      }
    >
  >({})

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState<boolean>(false)
  const [extractingPost, setExtractingPost] = useState<BlogPost | null>(null)

  // Create post form
  const [newDest, setNewDest] = useState<string>("")
  const [newTitle, setNewTitle] = useState<string>("")
  const [newContent, setNewContent] = useState<string>("")
  const [newPhotos, setNewPhotos] = useState<string>("")
  const [submittingPost, setSubmittingPost] = useState<boolean>(false)

  // Initial load
  useEffect(() => {
    loadDestinations()
    loadPosts(true)
  }, [selectedDest])

  const loadDestinations = async () => {
    try {
      const dests = await blogApi.getDestinations()
      setDestinations(dests || [])
    } catch {
      // ignore
    }
  }

  const loadPosts = async (reset = false) => {
    setLoading(true)
    const targetPage = reset ? 0 : page
    try {
      const res = await blogApi.getPosts({
        destination: selectedDest === "ALL" ? undefined : selectedDest,
        search: searchQuery.trim() || undefined,
        page: targetPage,
        size: 10,
      })
      if (reset) {
        setPosts(res.content || [])
        setPage(0)
      } else {
        setPosts((prev) => [...prev, ...(res.content || [])])
      }
      setHasMore((targetPage + 1) * res.size < res.totalElements)
    } catch (err: any) {
      console.error("Load posts error:", err)
      toast.error("Không thể tải danh sách bài viết")
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    loadPosts(true)
  }

  // Handle post upvote
  const handleToggleUpvote = async (post: BlogPost) => {
    if (!user) {
      toast.error("Vui lòng đăng nhập để bình chọn bài viết!")
      return
    }

    try {
      const updated = await blogApi.togglePostUpvote(post.id)
      setPosts((prev) =>
        prev.map((p) =>
          p.id === post.id
            ? {
                ...p,
                upvoteCount: updated.upvoteCount,
                isUpvoted: updated.isUpvoted,
              }
            : p
        )
      )
    } catch (err: any) {
      toast.error(err.message || "Không thể thực hiện bình chọn")
    }
  }

  // Delete post
  const handleDeletePost = async (postId: number) => {
    if (!confirm("Bạn có chắc chắn muốn xóa bài viết này không?")) return
    try {
      await blogApi.deletePost(postId)
      setPosts((prev) => prev.filter((p) => p.id !== postId))
      toast.success("Đã xóa bài viết")
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi xóa bài viết")
    }
  }

  // Toggle comments view
  const toggleComments = async (postId: number) => {
    const current = commentStates[postId] || {
      comments: [],
      loading: false,
      isOpen: false,
      newText: "",
    }

    if (!current.isOpen && current.comments.length === 0) {
      setCommentStates((prev) => ({
        ...prev,
        [postId]: { ...current, isOpen: true, loading: true },
      }))
      try {
        const comments = await blogApi.getComments(postId)
        setCommentStates((prev) => ({
          ...prev,
          [postId]: { ...current, isOpen: true, loading: false, comments },
        }))
      } catch {
        setCommentStates((prev) => ({
          ...prev,
          [postId]: { ...current, isOpen: true, loading: false },
        }))
      }
    } else {
      setCommentStates((prev) => ({
        ...prev,
        [postId]: { ...current, isOpen: !current.isOpen },
      }))
    }
  }

  // Add comment
  const handleAddComment = async (postId: number) => {
    if (!user) {
      toast.error("Vui lòng đăng nhập để bình luận!")
      return
    }
    const current = commentStates[postId]
    if (!current || !current.newText.trim()) return

    try {
      const created = await blogApi.addComment(postId, current.newText.trim())
      setCommentStates((prev) => ({
        ...prev,
        [postId]: {
          ...current,
          comments: [...current.comments, created],
          newText: "",
        },
      }))
      setPosts((prev) =>
        prev.map((p) =>
          p.id === postId ? { ...p, commentCount: p.commentCount + 1 } : p
        )
      )
      toast.success("Đã gửi bình luận")
    } catch (err: any) {
      toast.error(err.message || "Không thể gửi bình luận")
    }
  }

  // Toggle comment upvote
  const handleToggleCommentUpvote = async (postId: number, commentId: number) => {
    if (!user) {
      toast.error("Vui lòng đăng nhập để thích bình luận!")
      return
    }
    try {
      const updated = await blogApi.toggleCommentUpvote(commentId)
      setCommentStates((prev) => {
        const postState = prev[postId]
        if (!postState) return prev
        return {
          ...prev,
          [postId]: {
            ...postState,
            comments: postState.comments.map((c) =>
              c.id === commentId
                ? { ...c, upvoteCount: updated.upvoteCount, isUpvoted: updated.isUpvoted }
                : c
            ),
          },
        }
      })
    } catch (err: any) {
      toast.error(err.message || "Không thể bình chọn bình luận")
    }
  }

  // Handle post creation
  const handleCreatePost = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newDest.trim() || !newTitle.trim() || !newContent.trim()) {
      toast.error("Vui lòng nhập đầy đủ điểm đến, tiêu đề và nội dung bài viết!")
      return
    }

    setSubmittingPost(true)
    try {
      const photoUrls = newPhotos
        .split("\n")
        .map((p) => p.trim())
        .filter((p) => p.length > 0)

      const request: BlogPostRequest = {
        destinationName: newDest.trim(),
        title: newTitle.trim(),
        content: newContent.trim(),
        photoUrls,
      }

      const created = await blogApi.createPost(request)
      setPosts((prev) => [created, ...prev])
      setIsCreateOpen(false)
      setNewDest("")
      setNewTitle("")
      setNewContent("")
      setNewPhotos("")
      toast.success("Đăng bài chia sẻ thành công!")
      loadDestinations()
    } catch (err: any) {
      toast.error(err.message || "Không thể đăng bài viết")
    } finally {
      setSubmittingPost(false)
    }
  }

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8 max-w-5xl">
      {/* Hero Header */}
      <div className="relative rounded-3xl p-6 sm:p-10 mb-8 overflow-hidden bg-gradient-to-br from-primary/10 via-amber-500/10 to-background border border-border/70 shadow-sm">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30 mb-3">
            <Sparkles className="h-3.5 w-3.5 text-amber-500" />
            <span>Cộng Đồng Tri Thức Du Lịch TripMind</span>
          </div>
          <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-foreground leading-tight">
            Kinh Nghiệm &amp; Review Thực Tế
          </h1>
          <p className="mt-2 text-sm sm:text-base text-muted-foreground leading-relaxed">
            Chia sẻ đánh giá, quán ăn chuẩn vị và kinh nghiệm độc đáo của bạn. Các bài viết và bình luận chất lượng cao sẽ được{" "}
            <span className="font-semibold text-amber-600 dark:text-amber-400">
              Admin &amp; AI đúc kết thành Dataset
            </span>{" "}
            để gợi ý trực tiếp vào lịch trình tự động của TripMind!
          </p>

          <div className="flex flex-wrap items-center gap-3 mt-6">
            <Button
              onClick={() => {
                if (!user) {
                  toast.error("Vui lòng đăng nhập để viết bài!")
                  return
                }
                setIsCreateOpen(true)
              }}
              className="gap-2 font-semibold shadow-md shadow-primary/20"
            >
              <Plus className="h-4 w-4" /> Viết bài chia sẻ
            </Button>

            {user?.role === "ADMIN" && (
              <div className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-700 dark:text-amber-300 font-medium">
                <ShieldCheck className="h-4 w-4 text-amber-500" />
                <span>Bạn có quyền Admin: Có thể bấm nút <b>⚡ Trích xuất Dataset AI</b> bên cạnh Upvote</span>
              </div>
            )}
          </div>
        </div>

        {/* Decorative background circle */}
        <div className="absolute right-0 top-0 w-96 h-96 bg-gradient-to-bl from-amber-500/20 to-primary/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />
      </div>

      {/* Filter and Search Bar */}
      <div className="space-y-4 mb-8">
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm kiếm bài viết theo từ khóa, quán ăn, mẹo di chuyển..."
              className="pl-10 h-10 bg-background/80"
            />
          </div>
          <Button type="submit" variant="secondary" className="gap-1.5">
            <Search className="h-4 w-4" /> Tìm
          </Button>
        </form>

        {/* Destination Filter Chips */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none text-xs">
          <button
            onClick={() => setSelectedDest("ALL")}
            className={`px-3.5 py-1.5 rounded-full font-medium transition-all whitespace-nowrap cursor-pointer ${
              selectedDest === "ALL"
                ? "bg-primary text-primary-foreground shadow-sm"
                : "bg-muted/70 hover:bg-muted text-muted-foreground hover:text-foreground"
            }`}
          >
            Tất cả điểm đến
          </button>
          {destinations.map((dest) => (
            <button
              key={dest}
              onClick={() => setSelectedDest(dest)}
              className={`px-3.5 py-1.5 rounded-full font-medium transition-all whitespace-nowrap flex items-center gap-1 cursor-pointer ${
                selectedDest === dest
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "bg-muted/70 hover:bg-muted text-muted-foreground hover:text-foreground"
              }`}
            >
              <MapPin className="h-3 w-3" />
              {dest}
            </button>
          ))}
        </div>
      </div>

      {/* Post List */}
      {loading && posts.length === 0 ? (
        <div className="py-20 flex flex-col items-center justify-center text-muted-foreground space-y-3">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm">Đang tải các bài chia sẻ cộng đồng...</p>
        </div>
      ) : posts.length === 0 ? (
        <Card className="p-12 text-center space-y-3 border-dashed">
          <Compass className="h-10 w-10 text-muted-foreground mx-auto" />
          <h3 className="font-bold text-base">Chưa có bài viết nào</h3>
          <p className="text-xs text-muted-foreground max-w-sm mx-auto">
            Hãy là người đầu tiên chia sẻ kinh nghiệm du lịch tại điểm đến này cho cộng đồng TripMind!
          </p>
          <Button
            size="sm"
            onClick={() => {
              if (!user) {
                toast.error("Vui lòng đăng nhập để viết bài!")
                return
              }
              setIsCreateOpen(true)
            }}
            className="gap-1.5 mt-2"
          >
            <Plus className="h-3.5 w-3.5" /> Viết bài ngay
          </Button>
        </Card>
      ) : (
        <div className="space-y-6">
          {posts.map((post) => {
            const commentsInfo = commentStates[post.id] || {
              comments: [],
              loading: false,
              isOpen: false,
              newText: "",
            }

            return (
              <Card
                key={post.id}
                className="overflow-hidden border-border/80 shadow-sm hover:shadow-md transition-all duration-200 bg-card"
              >
                <div className="p-5 sm:p-6 space-y-4">
                  {/* Post Author & Header Info */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-primary/30 to-amber-500/30 flex items-center justify-center font-bold text-sm text-foreground border border-border">
                        {post.user.name ? post.user.name.charAt(0).toUpperCase() : "U"}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-foreground">
                            {post.user.name}
                          </span>
                          {post.user.role === "ADMIN" && (
                            <span className="text-[10px] font-semibold px-1.5 py-0.2 rounded bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30">
                              Admin
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 text-xs text-muted-foreground mt-0.5">
                          <span className="flex items-center gap-1">
                            <Calendar className="h-3 w-3" /> {formatTimeAgo(post.createdAt)}
                          </span>
                          <span>&bull;</span>
                          <span className="flex items-center gap-1 font-medium text-primary">
                            <MapPin className="h-3 w-3" /> {post.destinationName}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* AI Dataset badge or delete button */}
                    <div className="flex items-center gap-2">
                      {(post.isConvertedToKnowledge || post.convertedToKnowledge) && (
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30">
                          <Sparkles className="h-3 w-3 text-amber-500" />
                          <span>Đã lưu vào Tri Thức AI</span>
                        </div>
                      )}

                      {(user?.id === post.user.id || user?.role === "ADMIN") && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleDeletePost(post.id)}
                          className="h-7 w-7 text-muted-foreground hover:text-rose-500"
                          title="Xóa bài viết"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      )}
                    </div>
                  </div>

                  {/* Post Title & Content */}
                  <div className="space-y-2">
                    <h2 className="text-base sm:text-lg font-bold text-foreground">
                      {post.title}
                    </h2>
                    <p className="text-xs sm:text-sm text-muted-foreground whitespace-pre-line leading-relaxed">
                      {post.content}
                    </p>
                  </div>

                  {/* Post Photos if any */}
                  {post.photoUrls && post.photoUrls.length > 0 && (
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pt-1">
                      {post.photoUrls.map((url, idx) => (
                        <a
                          key={idx}
                          href={url}
                          target="_blank"
                          rel="noreferrer"
                          className="relative aspect-video rounded-xl overflow-hidden bg-muted border border-border group block"
                        >
                          <img
                            src={url}
                            alt=""
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                            onError={(e) => {
                              // hide broken images
                              e.currentTarget.style.display = "none"
                            }}
                          />
                        </a>
                      ))}
                    </div>
                  )}

                  {/* Action Bar (Upvote, Admin Extract, Comments) */}
                  <div className="pt-3 border-t flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      {/* UPVOTE BUTTON */}
                      <Button
                        variant={(post.isUpvoted || post.upvoted) ? "default" : "outline"}
                        size="sm"
                        onClick={() => handleToggleUpvote(post)}
                        className={`h-8 gap-1.5 text-xs font-semibold transition-all ${
                          (post.isUpvoted || post.upvoted)
                            ? "bg-rose-500 hover:bg-rose-600 text-white border-rose-500 shadow-sm"
                            : "hover:text-rose-500 hover:border-rose-500/50"
                        }`}
                      >
                        <Heart
                          className={`h-3.5 w-3.5 ${
                            (post.isUpvoted || post.upvoted) ? "fill-current" : ""
                          }`}
                        />
                        <span>Hữu ích ({post.upvoteCount})</span>
                      </Button>

                      {/* ADMIN-ONLY: EXTRACT TO DATASET BUTTON NEXT TO UPVOTE */}
                      {user?.role === "ADMIN" && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setExtractingPost(post)}
                          className={`h-8 gap-1.5 text-xs font-semibold transition-all ${
                            (post.isConvertedToKnowledge || post.convertedToKnowledge)
                              ? "border-amber-500/40 text-amber-700 dark:text-amber-300 bg-amber-500/10 hover:bg-amber-500/20"
                              : "border-amber-500/60 bg-gradient-to-r from-amber-500/15 to-orange-500/15 text-amber-700 dark:text-amber-300 hover:from-amber-500/25 hover:to-orange-500/25 shadow-xs"
                          }`}
                          title="Trích xuất bài viết và thảo luận thành tri thức Dataset cho AI"
                        >
                          <Sparkles className="h-3.5 w-3.5 text-amber-500 animate-pulse" />
                          <span>
                            {(post.isConvertedToKnowledge || post.convertedToKnowledge)
                              ? "Cập nhật Dataset AI"
                              : "⚡ Trích xuất Dataset AI"}
                          </span>
                        </Button>
                      )}

                      {/* COMMENTS BUTTON */}
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => toggleComments(post.id)}
                        className="h-8 gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                      >
                        <MessageSquare className="h-3.5 w-3.5" />
                        <span>Bình luận ({post.commentCount})</span>
                        {commentsInfo.isOpen ? (
                          <ChevronUp className="h-3 w-3 ml-0.5" />
                        ) : (
                          <ChevronDown className="h-3 w-3 ml-0.5" />
                        )}
                      </Button>
                    </div>

                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        navigator.clipboard.writeText(
                          window.location.origin + `/community?post=${post.id}`
                        )
                        toast.success("Đã sao chép liên kết bài viết!")
                      }}
                      className="h-8 gap-1 text-xs text-muted-foreground hover:text-foreground"
                    >
                      <Share2 className="h-3.5 w-3.5" />
                      <span className="hidden sm:inline">Chia sẻ</span>
                    </Button>
                  </div>

                  {/* COMMENTS SECTION */}
                  {commentsInfo.isOpen && (
                    <div className="pt-4 border-t space-y-3 bg-muted/20 -mx-5 -mb-5 sm:-mx-6 sm:-mb-6 p-4 sm:p-5 rounded-b-xl animate-in fade-in duration-200">
                      <h4 className="text-xs font-bold text-foreground flex items-center gap-1.5">
                        <MessageSquare className="h-3.5 w-3.5 text-primary" />
                        <span>Thảo luận cộng đồng ({commentsInfo.comments.length})</span>
                      </h4>

                      {/* Comment Input */}
                      <div className="flex gap-2">
                        <Input
                          placeholder="Viết cảm nghĩ hoặc chia sẻ thêm mẹo của bạn..."
                          value={commentsInfo.newText}
                          onChange={(e) =>
                            setCommentStates((prev) => ({
                              ...prev,
                              [post.id]: { ...commentsInfo, newText: e.target.value },
                            }))
                          }
                          onKeyDown={(e) => {
                            if (e.key === "Enter" && !e.shiftKey) {
                              e.preventDefault()
                              handleAddComment(post.id)
                            }
                          }}
                          className="h-8.5 text-xs bg-background"
                        />
                        <Button
                          size="sm"
                          onClick={() => handleAddComment(post.id)}
                          disabled={!commentsInfo.newText.trim()}
                          className="h-8.5 gap-1 text-xs font-semibold shrink-0"
                        >
                          <Send className="h-3 w-3" /> Gửi
                        </Button>
                      </div>

                      {/* Comments List */}
                      {commentsInfo.loading ? (
                        <div className="py-4 text-center text-xs text-muted-foreground flex items-center justify-center gap-2">
                          <Loader2 className="h-3.5 w-3.5 animate-spin text-primary" />
                          <span>Đang tải bình luận...</span>
                        </div>
                      ) : commentsInfo.comments.length === 0 ? (
                        <p className="text-xs text-muted-foreground text-center py-2 italic">
                          Chưa có bình luận nào. Hãy bắt đầu cuộc trò chuyện!
                        </p>
                      ) : (
                        <div className="space-y-2.5 pt-1">
                          {commentsInfo.comments.map((comment) => (
                            <div
                              key={comment.id}
                              className="p-3 rounded-xl bg-background border border-border/80 text-xs space-y-1.5"
                            >
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-1.5">
                                  <span className="font-bold text-foreground">
                                    {comment.user.name}
                                  </span>
                                  {comment.user.role === "ADMIN" && (
                                    <span className="text-[9px] font-semibold px-1 py-0.2 rounded bg-amber-500/15 text-amber-700 dark:text-amber-300">
                                      Admin
                                    </span>
                                  )}
                                  <span className="text-[10px] text-muted-foreground">
                                    &bull; {formatTimeAgo(comment.createdAt)}
                                  </span>
                                </div>

                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() =>
                                    handleToggleCommentUpvote(post.id, comment.id)
                                  }
                                  className={`h-6 px-1.5 text-[11px] gap-1 ${
                                    (comment.isUpvoted || comment.upvoted)
                                      ? "text-rose-500 font-semibold"
                                      : "text-muted-foreground hover:text-rose-500"
                                  }`}
                                >
                                  <Heart
                                    className={`h-3 w-3 ${
                                      (comment.isUpvoted || comment.upvoted) ? "fill-current" : ""
                                    }`}
                                  />
                                  <span>{comment.upvoteCount}</span>
                                </Button>
                              </div>

                              <p className="text-muted-foreground leading-relaxed whitespace-pre-line text-xs">
                                {comment.content}
                              </p>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </Card>
            )
          })}

          {hasMore && (
            <div className="text-center pt-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setPage((p) => p + 1)
                  loadPosts(false)
                }}
                disabled={loading}
                className="gap-2 text-xs"
              >
                {loading && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                Xem thêm bài viết
              </Button>
            </div>
          )}
        </div>
      )}

      {/* CREATE POST MODAL */}
      {isCreateOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
          <Card className="w-full max-w-lg border-border/80 shadow-2xl bg-card overflow-hidden max-h-[90vh] flex flex-col">
            <div className="p-4 border-b flex items-center justify-between bg-muted/40 shrink-0">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-primary/10 text-primary">
                  <Plus className="h-4 w-4" />
                </div>
                <h3 className="font-bold text-sm">Viết bài chia sẻ kinh nghiệm</h3>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsCreateOpen(false)}
                className="rounded-full h-7 w-7"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>

            <form onSubmit={handleCreatePost} className="p-5 space-y-3.5 text-xs overflow-y-auto flex-1">
              <div className="space-y-1">
                <label className="font-semibold text-foreground flex items-center gap-1">
                  <MapPin className="h-3 w-3 text-primary" /> Điểm đến / Thành phố *
                </label>
                <Input
                  required
                  placeholder="VD: Da Nang, Hanoi, Phu Quoc, Hoi An..."
                  value={newDest}
                  onChange={(e) => setNewDest(e.target.value)}
                  className="h-8.5 text-xs bg-background"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-foreground">Tiêu đề bài viết *</label>
                <Input
                  required
                  placeholder="VD: Top 5 quán bánh xèo chuẩn vị người bản địa Đà Nẵng..."
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="h-8.5 text-xs bg-background"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-foreground">
                  Nội dung đánh giá &amp; kinh nghiệm thực tế *
                </label>
                <textarea
                  required
                  rows={6}
                  placeholder="Chia sẻ chi tiết: Tên quán/địa điểm, giá cả, thời gian nên đi, mẹo tránh xếp hàng hoặc lưu ý cho chuyến đi..."
                  value={newContent}
                  onChange={(e) => setNewContent(e.target.value)}
                  className="w-full rounded-lg border border-border bg-background p-2.5 text-xs outline-none focus:ring-1 focus:ring-primary resize-none leading-relaxed"
                />
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-foreground flex items-center gap-1">
                  <ImageIcon className="h-3 w-3 text-muted-foreground" /> Link hình ảnh (mỗi dòng 1 link URL, tùy chọn)
                </label>
                <textarea
                  rows={2}
                  placeholder="https://images.unsplash.com/..."
                  value={newPhotos}
                  onChange={(e) => setNewPhotos(e.target.value)}
                  className="w-full rounded-lg border border-border bg-background p-2 text-xs outline-none focus:ring-1 focus:ring-primary resize-none font-mono"
                />
              </div>

              <div className="pt-3 border-t flex items-center justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsCreateOpen(false)}
                  disabled={submittingPost}
                  className="text-xs"
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={submittingPost}
                  className="text-xs font-semibold gap-1.5"
                >
                  {submittingPost ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 animate-spin" /> Đang đăng...
                    </>
                  ) : (
                    <>
                      <Send className="h-3.5 w-3.5" /> Đăng bài
                    </>
                  )}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* ADMIN AI EXTRACTION MODAL */}
      {extractingPost && (
        <ExtractKnowledgeModal
          post={extractingPost}
          isOpen={true}
          onClose={() => setExtractingPost(null)}
          onSuccess={(convertedId) => {
            setPosts((prev) =>
              prev.map((p) =>
                p.id === extractingPost.id
                  ? {
                      ...p,
                      isConvertedToKnowledge: true,
                      convertedKnowledgeId: convertedId,
                    }
                  : p
              )
            )
          }}
        />
      )}
    </div>
  )
}
