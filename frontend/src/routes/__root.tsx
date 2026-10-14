import { NotFound } from "@/components/common/NotFound"
import { Toaster } from "@/components/ui/sonner"
import { AuthProvider } from "@/contexts/auth-context"
import { ThemeProvider } from "@/contexts/theme-context"
import { Outlet, createRootRoute } from "@tanstack/react-router"

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
