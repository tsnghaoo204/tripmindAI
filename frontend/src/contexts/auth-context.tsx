import type { User } from "@/client/types"
import { getStoredUser, getToken, logout as logoutSession, saveSession, subscribeAuth } from "@/lib/auth-storage"
import * as React from "react"

interface AuthContextType {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  login: (token: string, user: User) => void
  logout: () => void
}

const AuthContext = React.createContext<AuthContextType | undefined>(undefined)

function readSession() {
  return { token: getToken(), user: getStoredUser() }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = React.useState(readSession)

  // Keep in sync with logouts triggered outside React (e.g. 401 interceptor)
  React.useEffect(() => subscribeAuth(() => setSession(readSession())), [])

  const login = React.useCallback((newToken: string, newUser: User) => {
    saveSession(newToken, newUser)
  }, [])

  const value = React.useMemo<AuthContextType>(
    () => ({
      user: session.token ? session.user : null,
      token: session.token,
      isAuthenticated: !!session.token,
      login,
      logout: logoutSession,
    }),
    [session, login],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = React.useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
