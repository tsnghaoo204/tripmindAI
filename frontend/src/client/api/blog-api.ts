import { request } from "../client"
import {
  BlogPost,
  BlogComment,
  BlogPostRequest,
  ExtractedKnowledge,
  DestinationKnowledge,
  KnowledgeRequest,
} from "../types"

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
}

export const blogApi = {
  getPosts: async (params?: {
    destination?: string
    search?: string
    page?: number
    size?: number
  }): Promise<PageResponse<BlogPost>> => {
    const q = new URLSearchParams()
    if (params?.destination) q.set("destination", params.destination)
    if (params?.search) q.set("search", params.search)
    if (params?.page !== undefined) q.set("page", String(params.page))
    if (params?.size !== undefined) q.set("size", String(params.size))
    return request<PageResponse<BlogPost>>(`/api/blog/posts?${q.toString()}`)
  },

  getPostDetail: async (id: number): Promise<BlogPost> => {
    return request<BlogPost>(`/api/blog/posts/${id}`)
  },

  getDestinations: async (): Promise<string[]> => {
    return request<string[]>("/api/blog/destinations")
  },

  createPost: async (data: BlogPostRequest): Promise<BlogPost> => {
    return request<BlogPost>("/api/blog/posts", {
      method: "POST",
      data,
    })
  },

  deletePost: async (id: number): Promise<void> => {
    return request<void>(`/api/blog/posts/${id}`, {
      method: "DELETE",
    })
  },

  togglePostUpvote: async (id: number): Promise<BlogPost> => {
    return request<BlogPost>(`/api/blog/posts/${id}/upvote`, {
      method: "POST",
    })
  },

  getComments: async (postId: number): Promise<BlogComment[]> => {
    return request<BlogComment[]>(`/api/blog/posts/${postId}/comments`)
  },

  addComment: async (postId: number, content: string): Promise<BlogComment> => {
    return request<BlogComment>(`/api/blog/posts/${postId}/comments`, {
      method: "POST",
      data: { content },
    })
  },

  deleteComment: async (commentId: number): Promise<void> => {
    return request<void>(`/api/blog/comments/${commentId}`, {
      method: "DELETE",
    })
  },

  toggleCommentUpvote: async (commentId: number): Promise<BlogComment> => {
    return request<BlogComment>(`/api/blog/comments/${commentId}/upvote`, {
      method: "POST",
    })
  },

  // Admin AI Extraction & Conversion
  extractKnowledge: async (postId: number): Promise<ExtractedKnowledge> => {
    return request<ExtractedKnowledge>(`/api/admin/blog/posts/${postId}/extract-knowledge`, {
      method: "POST",
    })
  },

  convertToKnowledge: async (postId: number, data: KnowledgeRequest): Promise<DestinationKnowledge> => {
    return request<DestinationKnowledge>(`/api/admin/blog/posts/${postId}/convert-to-knowledge`, {
      method: "POST",
      data,
    })
  },
}
