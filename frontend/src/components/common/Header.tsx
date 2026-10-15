import { UserProfileModal } from "@/components/pages/user/UserProfileModal"
import { Button } from "@/components/ui/button"
import { Dialog, DialogDescription, DialogTitle, SheetContent } from "@/components/ui/dialog"
import { useAuth } from "@/contexts/auth-context"
import { Link } from "@tanstack/react-router"
import { Compass, LogOut, MapPin, Menu, Moon, Settings, ShieldCheck, Sun, Users, X } from "lucide-react"
import { useTheme } from "next-themes"
import { useState } from "react"
import { Logo } from "./Logo"

export function Header() {
  const { user, logout } = useAuth()
  const { theme, setTheme } = useTheme()
  const [isProfileOpen, setIsProfileOpen] = useState(false)
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false)

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

        <div className="flex items-center gap-2 sm:gap-3">
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
                    <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30 hidden sm:inline-block">
                      Admin
                    </span>
                  )}
                  <Settings className="h-3.5 w-3.5 text-muted-foreground group-hover:text-foreground transition-colors ml-0.5 hidden sm:inline-block" />
                </div>
              </button>
              <Button
                variant="ghost"
                size="icon"
                onClick={logout}
                title="Đăng xuất"
                aria-label="Đăng xuất"
                className="hidden sm:inline-flex"
              >
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <div className="hidden sm:flex items-center gap-2">
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

          {/* Hamburger Menu Toggle for Mobile */}
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setIsMobileNavOpen(true)}
            className="md:hidden rounded-full"
            title="Mở menu"
            aria-label="Mở menu điều hướng"
          >
            <Menu className="h-5 w-5" />
          </Button>
        </div>
      </div>

      {/* Mobile Navigation Drawer */}
      <Dialog open={isMobileNavOpen} onOpenChange={setIsMobileNavOpen}>
        <SheetContent className="w-[85vw] max-w-xs p-0 flex flex-col justify-between">
          <DialogTitle className="sr-only">Menu Điều Hướng</DialogTitle>
          <DialogDescription className="sr-only">Danh sách liên kết điều hướng trên thiết bị di động</DialogDescription>

          <div>
            <div className="flex items-center justify-between p-4 border-b border-border/60">
              <Logo />
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsMobileNavOpen(false)}
                className="rounded-full h-8 w-8"
                aria-label="Đóng menu"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>

            <nav className="p-3 space-y-1">
              <Link
                to="/"
                onClick={() => setIsMobileNavOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-all [&.active]:bg-primary/10 [&.active]:text-primary [&.active]:font-semibold"
              >
                <Compass className="h-4 w-4" />
                <span>Chuyến đi của tôi</span>
              </Link>
              <Link
                to="/places"
                onClick={() => setIsMobileNavOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-all [&.active]:bg-primary/10 [&.active]:text-primary [&.active]:font-semibold"
              >
                <MapPin className="h-4 w-4" />
                <span>Khám phá địa điểm</span>
              </Link>
              <Link
                to="/community"
                onClick={() => setIsMobileNavOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-all [&.active]:bg-primary/10 [&.active]:text-primary [&.active]:font-semibold"
              >
                <Users className="h-4 w-4" />
                <span>Cộng đồng &amp; Review</span>
              </Link>
              {user?.role === "ADMIN" && (
                <Link
                  to="/admin"
                  onClick={() => setIsMobileNavOpen(false)}
                  className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 transition-all [&.active]:bg-amber-500/15 [&.active]:font-semibold"
                >
                  <ShieldCheck className="h-4 w-4" />
                  <span>Quản trị Hệ Thống</span>
                </Link>
              )}
            </nav>
          </div>

          {/* Bottom account / auth section in drawer */}
          <div className="p-4 border-t border-border/60 space-y-3 bg-muted/20">
            {user ? (
              <div className="space-y-2.5">
                <div className="flex items-center gap-2.5 px-1">
                  <div className="h-9 w-9 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center text-primary font-bold text-sm overflow-hidden shrink-0">
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
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-semibold truncate text-foreground">{user.name}</div>
                    <div className="text-xs text-muted-foreground truncate">{user.email}</div>
                  </div>
                  {user.role === "ADMIN" && (
                    <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30 shrink-0">
                      Admin
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-2 pt-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setIsMobileNavOpen(false)
                      setIsProfileOpen(true)
                    }}
                    className="text-xs gap-1.5 justify-center"
                  >
                    <Settings className="h-3.5 w-3.5" />
                    Cài đặt
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setIsMobileNavOpen(false)
                      logout()
                    }}
                    className="text-xs gap-1.5 justify-center text-rose-500 hover:text-rose-600 hover:bg-rose-500/10"
                  >
                    <LogOut className="h-3.5 w-3.5" />
                    Đăng xuất
                  </Button>
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                <Link to="/login" onClick={() => setIsMobileNavOpen(false)}>
                  <Button variant="outline" className="w-full text-sm">
                    Đăng nhập
                  </Button>
                </Link>
                <Link to="/register" onClick={() => setIsMobileNavOpen(false)}>
                  <Button className="w-full text-sm">Đăng ký tài khoản</Button>
                </Link>
              </div>
            )}
          </div>
        </SheetContent>
      </Dialog>

      <UserProfileModal open={isProfileOpen} onOpenChange={setIsProfileOpen} />
    </header>
  )
}
