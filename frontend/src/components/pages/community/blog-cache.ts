import type { BlogPost, PageResponse } from "@/client/types"
import type { InfiniteData, QueryClient } from "@tanstack/react-query"

type PostPages = InfiniteData<PageResponse<BlogPost>, unknown>

/** Applies `fn` to every cached post list page; returning null drops the post. */
export function updateCachedPosts(queryClient: QueryClient, fn: (post: BlogPost) => BlogPost | null) {
  queryClient.setQueriesData<PostPages>({ queryKey: ["blog", "posts"] }, (data) =>
    data
      ? {
          ...data,
          pages: data.pages.map((page) => ({
            ...page,
            content: page.content.flatMap((p) => {
              const next = fn(p)
              return next ? [next] : []
            }),
          })),
        }
      : data,
  )
}

/** Flattens pages, skipping posts already seen on an earlier page (pages shift when new posts arrive). */
export function flattenPosts(data: PostPages | undefined): BlogPost[] {
  if (!data) return []
  const seen = new Set<number>()
  const result: BlogPost[] = []
  for (const page of data.pages) {
    for (const post of page.content) {
      if (seen.has(post.id)) continue
      seen.add(post.id)
      result.push(post)
    }
  }
  return result
}

export function nextPageParam(last: PageResponse<unknown>): number | undefined {
  return (last.page + 1) * last.size < last.totalElements ? last.page + 1 : undefined
}

export function formatTimeAgo(dateStr: string, now = Date.now()): string {
  const time = new Date(dateStr).getTime()
  if (Number.isNaN(time)) return dateStr
  const diffMins = Math.floor((now - time) / 60000)
  if (diffMins < 1) return "Vừa xong"
  if (diffMins < 60) return `${diffMins} phút trước`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours} giờ trước`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 30) return `${diffDays} ngày trước`
  return new Date(dateStr).toLocaleDateString("vi-VN")
}
