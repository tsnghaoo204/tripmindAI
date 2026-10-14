import type { User } from "@/client/types"

const TOKEN_KEY = "access_token"
const USER_KEY = "user_profile"

type Listener = () => void
const listeners = new Set<Listener>()
let unauthorizedHandler: (() => void) | null = null

function storage(): Storage | null {
  try {
    return typeof localStorage === "undefined" ? null : localStorage
  } catch {
    return null
  }
}

export function getToken(): string | null {
  return storage()?.getItem(TOKEN_KEY) ?? null
}

export function getStoredUser(): User | null {
  const s = storage()
  if (!s?.getItem(TOKEN_KEY)) return null
  const raw = s.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export function saveSession(token: string, user: User) {
  const s = storage()
  s?.setItem(TOKEN_KEY, token)
  s?.setItem(USER_KEY, JSON.stringify(user))
  notify()
}

export function clearSession() {
  const s = storage()
  s?.removeItem(TOKEN_KEY)
  s?.removeItem(USER_KEY)
  notify()
}

/** Clears the session and sends the user to the login page. */
export function logout() {
  clearSession()
  unauthorizedHandler?.()
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

export function subscribeAuth(listener: Listener): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

function notify() {
  for (const l of listeners) l()
}
