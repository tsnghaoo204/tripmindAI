import { blogApi } from "@/client/api/blog-api"
import { getErrorMessage } from "@/client/errors"
import type { BlogPost } from "@/client/types"
import { CreatePostDialog } from "@/components/pages/community/CreatePostDialog"
import { ExtractKnowledgeModal } from "@/components/pages/community/ExtractKnowledgeModal"
import { PostCard } from "@/components/pages/community/PostCard"
import { flattenPosts, nextPageParam, updateCachedPosts } from "@/components/pages/community/blog-cache"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { ConfirmDialog } from "@/components/ui/confirm-dialog"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/contexts/auth-context"
import { queryKeys } from "@/hooks/query-keys"
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import { Compass, Loader2, MapPin, Plus, Search, ShieldCheck, Sparkles } from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"

export const Route = createFileRoute("/_layout/community")({
  component: CommunityPage,
})

const PAGE_SIZE = 10

function CommunityPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const isAdmin = user?.role === "ADMIN"

  const [selectedDest, setSelectedDest] = useState<string>("ALL")
  const [searchInput, setSearchInput] = useState("")
  const [search, setSearch] = useState("")
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [extractingPost, setExtractingPost] = useState<BlogPost | null>(null)
  const [deletingPost, setDeletingPost] = useState<BlogPost | null>(null)

  const filters = { destination: selectedDest === "ALL" ? undefined : selectedDest, search: search || undefined }

  const { data: destinations = [] } = useQuery({
    queryKey: queryKeys.blogDestinations,
    queryFn: blogApi.getDestinations,
  })

  const postsQuery = useInfiniteQuery({
    queryKey: queryKeys.blogPosts(filters),
    queryFn: ({ pageParam }) => blogApi.getPosts({ ...filters, page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  })
  const posts = flattenPosts(postsQuery.data)

  const upvoteMutation = useMutation({
    mutationFn: (post: BlogPost) => blogApi.togglePostUpvote(post.id),
    onSuccess: (updated) =>
      updateCachedPosts(queryClient, (p) =>
        p.id === updated.id ? { ...p, upvoteCount: updated.upvoteCount, isUpvoted: updated.isUpvoted } : p,
      ),
    onError: (err) => toast.error(getErrorMessage(err, "Không thể thực hiện bình chọn")),
  })

  const deleteMutation = useMutation({
    mutationFn: (post: BlogPost) => blogApi.deletePost(post.id),
    onSuccess: (_res, post) => {
      setDeletingPost(null)
      updateCachedPosts(queryClient, (p) => (p.id === post.id ? null : p))
      queryClient.invalidateQueries({ queryKey: queryKeys.blogDestinations })
      toast.success("Đã xóa bài viết")
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi xóa bài viết")),
  })

  const openCreate = () => {
    if (!user) {
      toast.error("Vui lòng đăng nhập để viết bài!")
      return
    }
    setIsCreateOpen(true)
  }

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setSearch(searchInput.trim())
  }

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8 max-w-5xl">
      <div className="relative rounded-3xl p-6 sm:p-10 mb-8 overflow-hidden bg-gradient-to-br from-primary/10 via-amber-500/10 to-background border border-border/70 shadow-sm">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30 mb-3">
            <Sparkles className="h-3.5 w-3.5 text-amber-500" aria-hidden />
            <span>Cộng Đồng Tri Thức Du Lịch TripMind</span>
          </div>
          <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-foreground leading-tight">
            Kinh Nghiệm &amp; Review Thực Tế
          </h1>
          <p className="mt-2 text-sm sm:text-base text-muted-foreground leading-relaxed">
            Chia sẻ đánh giá, quán ăn chuẩn vị và kinh nghiệm độc đáo của bạn. Các bài viết và bình luận chất lượng cao
            sẽ được{" "}
            <span className="font-semibold text-amber-600 dark:text-amber-400">
              Admin &amp; AI đúc kết thành Dataset
            </span>{" "}
            để gợi ý trực tiếp vào lịch trình tự động của TripMind!
          </p>

          <div className="flex flex-wrap items-center gap-3 mt-6">
            <Button onClick={openCreate} className="gap-2 font-semibold shadow-md shadow-primary/20">
              <Plus className="h-4 w-4" /> Viết bài chia sẻ
            </Button>
            {isAdmin && (
              <div className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-700 dark:text-amber-300 font-medium">
                <ShieldCheck className="h-4 w-4 text-amber-500" aria-hidden />
                <span>
                  Bạn có quyền Admin: Có thể bấm nút <b>⚡ Trích xuất Dataset AI</b> bên cạnh Upvote
                </span>
              </div>
            )}
          </div>
        </div>
        <div className="absolute right-0 top-0 w-96 h-96 bg-gradient-to-bl from-amber-500/20 to-primary/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />
      </div>

      <div className="space-y-4 mb-8">
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-2.5 h-4 w-4 text-muted-foreground" aria-hidden />
            <Input
              aria-label="Tìm kiếm bài viết"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Tìm kiếm bài viết theo từ khóa, quán ăn, mẹo di chuyển..."
              className="pl-10 h-10 bg-background/80"
            />
          </div>
          <Button type="submit" variant="secondary" className="gap-1.5">
            <Search className="h-4 w-4" /> Tìm
          </Button>
        </form>

        <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none text-xs">
          {["ALL", ...destinations].map((dest) => (
            <button
              type="button"
              key={dest}
              aria-pressed={selectedDest === dest}
              onClick={() => setSelectedDest(dest)}
              className={`px-3.5 py-1.5 rounded-full font-medium transition-all whitespace-nowrap flex items-center gap-1 cursor-pointer ${
                selectedDest === dest
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "bg-muted/70 hover:bg-muted text-muted-foreground hover:text-foreground"
              }`}
            >
              {dest === "ALL" ? (
                "Tất cả điểm đến"
              ) : (
                <>
                  <MapPin className="h-3 w-3" aria-hidden />
                  {dest}
                </>
              )}
            </button>
          ))}
        </div>
      </div>

      {postsQuery.isPending ? (
        <div className="py-20 flex flex-col items-center justify-center text-muted-foreground space-y-3">
          <Loader2 className="h-8 w-8 animate-spin text-primary" aria-hidden />
          <p className="text-sm">Đang tải các bài chia sẻ cộng đồng...</p>
        </div>
      ) : postsQuery.isError ? (
        <Card className="p-12 text-center text-sm text-muted-foreground">
          {getErrorMessage(postsQuery.error, "Không thể tải danh sách bài viết")}
        </Card>
      ) : posts.length === 0 ? (
        <Card className="p-12 text-center space-y-3 border-dashed">
          <Compass className="h-10 w-10 text-muted-foreground mx-auto" aria-hidden />
          <h3 className="font-bold text-base">Chưa có bài viết nào</h3>
          <p className="text-xs text-muted-foreground max-w-sm mx-auto">
            Hãy là người đầu tiên chia sẻ kinh nghiệm du lịch tại điểm đến này cho cộng đồng TripMind!
          </p>
          <Button size="sm" onClick={openCreate} className="gap-1.5 mt-2">
            <Plus className="h-3.5 w-3.5" /> Viết bài ngay
          </Button>
        </Card>
      ) : (
        <div className="space-y-6">
          {posts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              canInteract={!!user}
              canDelete={user?.id === post.user.id || isAdmin}
              isAdmin={isAdmin}
              upvoting={upvoteMutation.isPending && upvoteMutation.variables?.id === post.id}
              onUpvote={(p) => upvoteMutation.mutate(p)}
              onDelete={(p) => setDeletingPost(p)}
              onExtract={setExtractingPost}
            />
          ))}

          {postsQuery.hasNextPage && (
            <div className="text-center pt-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => postsQuery.fetchNextPage()}
                disabled={postsQuery.isFetchingNextPage}
                className="gap-2 text-xs"
              >
                {postsQuery.isFetchingNextPage && <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden />}
                Xem thêm bài viết
              </Button>
            </div>
          )}
        </div>
      )}

      <CreatePostDialog
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={() => queryClient.invalidateQueries({ queryKey: ["blog"] })}
      />

      {extractingPost && (
        <ExtractKnowledgeModal
          post={extractingPost}
          isOpen
          onClose={() => setExtractingPost(null)}
          onSuccess={(convertedId) => {
            const id = extractingPost.id
            queryClient.invalidateQueries({ queryKey: ["admin", "knowledge"] })
            updateCachedPosts(queryClient, (p) =>
              p.id === id ? { ...p, isConvertedToKnowledge: true, convertedKnowledgeId: convertedId } : p,
            )
          }}
        />
      )}

      <ConfirmDialog
        open={deletingPost !== null}
        onOpenChange={(open) => !open && setDeletingPost(null)}
        title="Xóa bài viết"
        description="Bạn có chắc chắn muốn xóa bài viết này không? Hành động này không thể hoàn tác."
        confirmText="Xóa bài viết"
        variant="destructive"
        isLoading={deleteMutation.isPending}
        onConfirm={() => {
          if (deletingPost) deleteMutation.mutate(deletingPost)
        }}
      />
    </div>
  )
}
