import { createFileRoute, Outlet } from "@tanstack/react-router"
import { Header } from "@/components/common/Header"

export const Route = createFileRoute("/_layout")({
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
