import { createRootRoute, Outlet } from "@tanstack/react-router"
import { ThemeProvider } from "@/contexts/theme-context"
import { AuthProvider } from "@/contexts/auth-context"
import { Toaster } from "@/components/ui/sonner"
import { NotFound } from "@/components/common/NotFound"

export const Route = createRootRoute({
  component: RootComponent,
  notFoundComponent: NotFound,
})

function RootComponent() {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
      <AuthProvider>
        <div className="min-h-screen flex flex-col bg-background text-foreground">
          <Outlet />
          <Toaster richColors position="top-right" />
        </div>
      </AuthProvider>
    </ThemeProvider>
  )
}
