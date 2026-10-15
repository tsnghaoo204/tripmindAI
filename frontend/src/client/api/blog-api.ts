import { request } from "../client"
import type {
  BlogComment,
  BlogPost,
  BlogPostRequest,
  DestinationKnowledge,
  ExtractedKnowledge,
  KnowledgeRequest,
  PageResponse,
} from "../types"

export type { PageResponse }

export const blogApi = {
  getPosts: (params?: { destination?: string; search?: string; page?: number; size?: number }) => {
    const q = new URLSearchParams()
    if (params?.destination) q.set("destination", params.destination)
    if (params?.search) q.set("search", params.search)
    if (params?.page !== undefined) q.set("page", String(params.page))
    if (params?.size !== undefined) q.set("size", String(params.size))
    return request<PageResponse<BlogPost>>(`/api/blog/posts?${q.toString()}`)
  },

  getPostDetail: (id: number) => request<BlogPost>(`/api/blog/posts/${id}`),

  getDestinations: () => request<string[]>("/api/blog/posts/destinations"),

  createPost: (data: BlogPostRequest) => request<BlogPost>("/api/blog/posts", { method: "POST", data }),

  deletePost: (id: number) => request<void>(`/api/blog/posts/${id}`, { method: "DELETE" }),

  togglePostUpvote: (id: number) => request<BlogPost>(`/api/blog/posts/${id}/upvote`, { method: "POST" }),

  getComments: (postId: number) => request<BlogComment[]>(`/api/blog/posts/${postId}/comments`),

  addComment: (postId: number, content: string) =>
    request<BlogComment>(`/api/blog/posts/${postId}/comments`, { method: "POST", data: { content } }),

  toggleCommentUpvote: (commentId: number) =>
    request<BlogComment>(`/api/blog/comments/${commentId}/upvote`, { method: "POST" }),

  extractKnowledge: (postId: number) =>
    request<ExtractedKnowledge>(`/api/admin/blog/posts/${postId}/extract-knowledge`, { method: "POST" }),

  convertToKnowledge: (postId: number, data: KnowledgeRequest) =>
    request<DestinationKnowledge>(`/api/admin/blog/posts/${postId}/convert-to-knowledge`, { method: "POST", data }),

  generateTripRecap: (tripId: number) =>
    request<import("../types").TripRecapResponse>(`/api/blog/trips/${tripId}/recap`, { method: "POST" }),
}
