import { apiClient, request } from "../client"
import type { User, AuthResponse } from "../types"

export interface UserPreferences {
  travelStyle?: string
  budgetPreference?: string
  preferences?: string[]
}

export const authApi = {
  login: async (credentials: { email: string; password: string }): Promise<AuthResponse> => {
    return request<AuthResponse>("/api/auth/login", {
      method: "POST",
      data: credentials,
    })
  },

  register: async (data: { name: string; email: string; password: string }): Promise<AuthResponse> => {
    return request<AuthResponse>("/api/auth/register", {
      method: "POST",
      data,
    })
  },

  getProfile: async (): Promise<User> => {
    return request<User>("/api/users/profile")
  },

  updateProfile: async (data: { name?: string; avatarUrl?: string }): Promise<User> => {
    return request<User>("/api/users/profile", {
      method: "PUT",
      data,
    })
  },

  changePassword: async (data: { oldPassword: string; newPassword: string }): Promise<void> => {
    return request<void>("/api/users/change-password", {
      method: "PUT",
      data,
    })
  },

  getPreferences: async (): Promise<UserPreferences> => {
    return request<UserPreferences>("/api/me/preferences")
  },

  updatePreferences: async (data: UserPreferences): Promise<UserPreferences> => {
    return request<UserPreferences>("/api/me/preferences", {
      method: "PUT",
      data,
    })
  },
}
