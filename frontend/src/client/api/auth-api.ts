import { saveSession } from "@/lib/auth-storage"
import { validateAvatarUrl } from "@/lib/validation"
import { request } from "../client"
import { ApiError } from "../errors"
import type { AuthResponse, BudgetPreference, TravelStyle, User } from "../types"

export interface UserPreferences {
  travelStyle?: TravelStyle | null
  budgetPreference?: BudgetPreference | null
  preferences?: string[] | null
}

export const authApi = {
  login: (credentials: { email: string; password: string }) =>
    request<AuthResponse>("/api/auth/login", { method: "POST", data: credentials }),

  register: (data: { name: string; email: string; password: string }) =>
    request<AuthResponse>("/api/auth/register", { method: "POST", data }),

  getProfile: () => request<User>("/api/users/profile"),

  updateProfile: (data: { name?: string; avatarUrl?: string }) => {
    const avatarError = data.avatarUrl !== undefined ? validateAvatarUrl(data.avatarUrl) : null
    if (avatarError) return Promise.reject(new ApiError(avatarError))
    return request<User>("/api/users/profile", { method: "PUT", data })
  },

  /** The old token is revoked server-side, so the returned session replaces it. */
  changePassword: async (data: { currentPassword: string; newPassword: string }) => {
    const res = await request<AuthResponse>("/api/users/change-password", { method: "PUT", data })
    saveSession(res.accessToken, res.user)
    return res
  },

  getPreferences: () => request<UserPreferences>("/api/me/preferences"),

  updatePreferences: (data: UserPreferences) =>
    request<UserPreferences>("/api/me/preferences", { method: "PUT", data }),
}
