import { UserProfileModal } from "@/components/pages/user/UserProfileModal"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/contexts/auth-context"
import { Link } from "@tanstack/react-router"
import { LogOut, Moon, Settings, ShieldCheck, Sun } from "lucide-react"
import { useTheme } from "next-themes"
import { useState } from "react"
import { Logo } from "./Logo"

export function Header() {
  const { user, logout } = useAuth()
  const { theme, setTheme } = useTheme()
  const [isProfileOpen, setIsProfileOpen] = useState(false)

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="container mx-auto flex h-16 items-center justify-between px-4 sm:px-6">
        <div className="flex items-center gap-8">
          <Link to="/">
            <Logo />
          </Link>
          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-muted-foreground">
            <Link
              to="/"
              className="hover:text-foreground transition-colors [&.active]:text-primary [&.active]:font-semibold"
            >
              Chuyến đi của tôi
            </Link>
            <Link
              to="/places"
              className="hover:text-foreground transition-colors [&.active]:text-primary [&.active]:font-semibold"
            >
              Khám phá địa điểm
            </Link>
            <Link
              to="/community"
              className="hover:text-foreground transition-colors [&.active]:text-primary [&.active]:font-semibold"
            >
              Cộng đồng &amp; Review
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
            aria-label="Đổi giao diện sáng/tối"
          >
            <Sun className="h-4 w-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
            <Moon className="absolute h-4 w-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
          </Button>

          {user ? (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsProfileOpen(true)}
                className="flex items-center gap-2 p-1 pl-1.5 pr-2.5 rounded-full hover:bg-muted/70 transition-colors border border-transparent hover:border-border cursor-pointer group text-left"
                title="Tài khoản cá nhân & Đổi mật khẩu"
              >
                <div className="h-7 w-7 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center text-primary font-bold text-xs overflow-hidden shrink-0">
                  {user.avatarUrl ? (
                    <img
                      src={user.avatarUrl}
                      alt={user.name}
                      className="h-full w-full object-cover"
                      onError={(e) => {
                        e.currentTarget.style.display = "none"
                      }}
                    />
                  ) : (
                    user.name?.charAt(0).toUpperCase() || "U"
                  )}
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-sm font-medium hidden sm:inline-block max-w-[120px] truncate">{user.name}</span>
                  {user.role === "ADMIN" && (
                    <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30">
                      Admin
                    </span>
                  )}
                  <Settings className="h-3.5 w-3.5 text-muted-foreground group-hover:text-foreground transition-colors ml-0.5 hidden sm:inline-block" />
                </div>
              </button>
              <Button variant="ghost" size="icon" onClick={logout} title="Đăng xuất" aria-label="Đăng xuất">
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  Đăng nhập
                </Button>
              </Link>
              <Link to="/register">
                <Button size="sm">Đăng ký</Button>
              </Link>
            </div>
          )}
        </div>
      </div>
      <UserProfileModal open={isProfileOpen} onOpenChange={setIsProfileOpen} />
    </header>
  )
}
