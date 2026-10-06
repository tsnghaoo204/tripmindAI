import { Link } from "@tanstack/react-router"
import { useAuth } from "@/contexts/auth-context"
import { Logo } from "./Logo"
import { Button } from "@/components/ui/button"
import { LogOut, Sun, Moon, Plus, ShieldCheck } from "lucide-react"
import { useTheme } from "next-themes"

export function Header() {
  const { user, logout } = useAuth()
  const { theme, setTheme } = useTheme()

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="container mx-auto flex h-16 items-center justify-between px-4 sm:px-6">
        <div className="flex items-center gap-8">
          <Link to="/">
            <Logo />
          </Link>
          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-muted-foreground">
            <Link to="/" className="hover:text-foreground transition-colors [&.active]:text-primary [&.active]:font-semibold">
              Chuyến đi của tôi
            </Link>
            <Link to="/places" className="hover:text-foreground transition-colors [&.active]:text-primary [&.active]:font-semibold">
              Khám phá địa điểm
            </Link>
            {user?.role === "ADMIN" && (
              <Link
                to="/admin"
                className="flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 hover:bg-amber-500/20 transition-all [&.active]:ring-2 [&.active]:ring-amber-500/40"
              >
                <ShieldCheck className="h-3.5 w-3.5" />
                Quản trị Hệ Thống
              </Link>
            )}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
            className="rounded-full"
            title="Đổi giao diện"
          >
            <Sun className="h-4 w-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
            <Moon className="absolute h-4 w-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
          </Button>

          {user ? (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium hidden sm:inline-block">
                  {user.name}
                </span>
                {user.role === "ADMIN" && (
                  <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30">
                    Admin
                  </span>
                )}
              </div>
              <Button variant="ghost" size="icon" onClick={logout} title="Đăng xuất">
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link to="/login">
                <Button variant="ghost" size="sm">Đăng nhập</Button>
              </Link>
              <Link to="/register">
                <Button size="sm">Đăng ký</Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
