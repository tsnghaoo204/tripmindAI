export const APP_CONFIG = {
  name: "TripMind AI",
  // Empty means same origin (Vite dev proxy forwards /api)
  apiBaseUrl: (import.meta.env.VITE_API_URL || "").replace(/\/+$/, ""),
}

export function apiUrl(path: string): string {
  return `${APP_CONFIG.apiBaseUrl}${path}`
}
