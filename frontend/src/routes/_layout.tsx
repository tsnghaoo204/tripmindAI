import { Header } from "@/components/common/Header"
import { getToken } from "@/lib/auth-storage"
import { Outlet, createFileRoute, redirect } from "@tanstack/react-router"

export const Route = createFileRoute("/_layout")({
  beforeLoad: ({ location }) => {
    if (!getToken()) {
      throw redirect({ to: "/login", search: { redirect: location.href } })
    }
  },
  component: LayoutComponent,
})

function LayoutComponent() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-border/60 py-6 text-center text-xs text-muted-foreground">
        <div className="container mx-auto px-4">
          TripMind AI — Trợ lý Thông Minh Lập Kế Hoạch Du Lịch &bull; Hỗ trợ Gemini ReAct Agent
        </div>
      </footer>
    </div>
  )
}
