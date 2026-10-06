import * as React from "react"
import type { User } from "@/client/types"

interface AuthContextType {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  login: (token: string, user: User) => void
  logout: () => void
}

const AuthContext = React.createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = React.useState<string | null>(() => localStorage.getItem("access_token"))
  const [user, setUser] = React.useState<User | null>(() => {
    const saved = localStorage.getItem("user_profile")
    return saved ? JSON.parse(saved) : null
  })

  const login = React.useCallback((newToken: string, newUser: User) => {
    localStorage.setItem("access_token", newToken)
    localStorage.setItem("user_profile", JSON.stringify(newUser))
    setToken(newToken)
    setUser(newUser)
  }, [])

  const logout = React.useCallback(() => {
    localStorage.removeItem("access_token")
    localStorage.removeItem("user_profile")
    setToken(null)
    setUser(null)
    window.location.href = "/login"
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = React.useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
