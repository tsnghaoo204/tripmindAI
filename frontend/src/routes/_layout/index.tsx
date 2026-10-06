import { createFileRoute, Link } from "@tanstack/react-router"
import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Plus, MapPin, Calendar, Users, Sparkles, Compass, Wand2 } from "lucide-react"
import { formatDateRange, formatCurrency } from "@/lib/utils"
import { tripApi } from "@/client/api/trip-api"
import type { Trip } from "@/client/types"
import { CreateTripModal } from "@/components/pages/trips/CreateTripModal"
import { toast } from "sonner"

export const Route = createFileRoute("/_layout/")({
  component: DashboardPage,
})

function DashboardPage() {
  const [filter, setFilter] = useState<"upcoming" | "past">("upcoming")
  const [trips, setTrips] = useState<Trip[]>([])
  const [loading, setLoading] = useState(true)
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [homePrompt, setHomePrompt] = useState("")

  const fetchTrips = async () => {
    try {
      setLoading(true)
      const data = await tripApi.getTrips(filter)
      setTrips(data || [])
    } catch {
      // nếu chưa đăng nhập hoặc lỗi kết nối
      setTrips([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchTrips()
  }, [filter])

  const handleTriggerPrompt = (text?: string) => {
    const promptToUse = text || homePrompt
    if (!promptToUse.trim()) {
      setIsCreateOpen(true)
      return
    }
    setHomePrompt(promptToUse)
    setIsCreateOpen(true)
  }

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8">
      {/* Top Banner / Welcome */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Kế hoạch Du lịch</h1>
          <p className="text-muted-foreground mt-1">
            Quản lý hành trình, tối ưu thứ tự lộ trình và lên lịch thông minh cùng Trợ lý AI Gemini.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            onClick={() => {
              setHomePrompt("")
              setIsCreateOpen(true)
            }}
            className="gap-2 shadow-md shadow-primary/20 bg-primary hover:bg-primary/90 cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            Tạo chuyến đi mới
          </Button>
        </div>
      </div>

      {/* AI Chat Prompt Hero Box */}
      <div className="p-5 sm:p-6 rounded-2xl bg-gradient-to-br from-primary/15 via-background to-amber-500/10 border border-primary/25 shadow-md mb-8 space-y-3.5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-primary text-primary-foreground shadow">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">
                Lập Kế Hoạch Bằng AI: Nhập mô tả tự nhiên
              </h2>
              <p className="text-xs text-muted-foreground">
                Nhập câu mô tả chuyến đi — Hệ thống sẽ tự động bóc tách điểm đến, ngày đi, ngân sách và điền sẵn vào giao diện!
              </p>
            </div>
          </div>
          <Badge className="bg-primary/20 text-primary border-primary/30 w-fit text-[11px] font-semibold">
            ✨ AI Auto-Parser
          </Badge>
        </div>

        <div className="flex flex-col sm:flex-row gap-2">
          <Input
            placeholder="VD: tôi muốn lên kế hoạch đi nha trang 2 ngày 1 đêm cho 2 người, ngân sách 4 triệu..."
            value={homePrompt}
            onChange={(e) => setHomePrompt(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault()
                handleTriggerPrompt()
              }
            }}
            className="text-xs sm:text-sm bg-background/90 h-10 sm:h-11 border-primary/30 focus-visible:ring-primary shadow-inner"
          />
          <Button
            type="button"
            onClick={() => handleTriggerPrompt()}
            className="h-10 sm:h-11 px-5 font-bold gap-2 shrink-0 bg-primary text-primary-foreground shadow-md hover:opacity-95 cursor-pointer"
          >
            <Wand2 className="h-4 w-4" />
            Phân tích &amp; Tạo ngay
          </Button>
        </div>

        {/* Quick prompt suggestions chips */}
        <div className="flex items-center gap-1.5 flex-wrap text-xs pt-1">
          <span className="text-muted-foreground text-[11px] font-medium">Gợi ý thử nhanh:</span>
          <button
            type="button"
            onClick={() => handleTriggerPrompt("tôi muốn lên kế hoạch đi nha trang 2 ngày 1 đêm")}
            className="px-2.5 py-1 rounded-full text-xs bg-background/90 border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
          >
            🏖️ Đi Nha Trang 2 ngày 1 đêm
          </button>
          <button
            type="button"
            onClick={() => handleTriggerPrompt("tôi muốn đi đà lạt 3 ngày 2 đêm cùng người yêu, ngân sách 5 triệu")}
            className="px-2.5 py-1 rounded-full text-xs bg-background/90 border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
          >
            🌸 Đi Đà Lạt 3N2Đ lãng mạn
          </button>
          <button
            type="button"
            onClick={() => handleTriggerPrompt("kế hoạch đi phú quốc 4 ngày 3 đêm cho gia đình có 2 bé nhỏ, ngân sách 15 triệu")}
            className="px-2.5 py-1 rounded-full text-xs bg-background/90 border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
          >
            🏝️ Đi Phú Quốc 4N3Đ gia đình
          </button>
          <button
            type="button"
            onClick={() => handleTriggerPrompt("đi quy nhơn 3 ngày 2 đêm 2 người, ngân sách 6 triệu, ăn chay")}
            className="px-2.5 py-1 rounded-full text-xs bg-background/90 border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
          >
            🌊 Đi Quy Nhơn 3N2Đ ăn chay
          </button>
          <button
            type="button"
            onClick={() => handleTriggerPrompt("lên kế hoạch đi côn đảo 3 ngày 2 đêm cho 4 người")}
            className="px-2.5 py-1 rounded-full text-xs bg-background/90 border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
          >
            📍 Đi Côn Đảo (Địa điểm khác)
          </button>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 border-b border-border mb-6">
        <button
          onClick={() => setFilter("upcoming")}
          className={`pb-3 px-3 text-sm font-medium border-b-2 transition-colors cursor-pointer ${
            filter === "upcoming"
              ? "border-primary text-primary font-semibold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          Sắp tới & Đang đi
        </button>
        <button
          onClick={() => setFilter("past")}
          className={`pb-3 px-3 text-sm font-medium border-b-2 transition-colors cursor-pointer ${
            filter === "past"
              ? "border-primary text-primary font-semibold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          Đã hoàn thành
        </button>
      </div>

      {/* Trips Grid / Empty State */}
      {loading ? (
        <div className="py-20 text-center text-sm text-muted-foreground">Đang tải danh sách chuyến đi...</div>
      ) : trips.length === 0 ? (
        <div className="py-16 text-center rounded-3xl border border-dashed border-border bg-muted/20 p-8 max-w-md mx-auto">
          <div className="p-4 rounded-full bg-primary/10 text-primary w-fit mx-auto mb-4">
            <Compass className="h-8 w-8" />
          </div>
          <h3 className="font-bold text-lg mb-1">Chưa có chuyến đi nào</h3>
          <p className="text-xs text-muted-foreground mb-6">
            Bắt đầu lên kế hoạch cho kỳ nghỉ sắp tới cùng Trợ lý AI TripMind ngay hôm nay!
          </p>
          <Button onClick={() => setIsCreateOpen(true)} className="gap-2">
            <Plus className="h-4 w-4" /> Tạo chuyến đi đầu tiên
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {trips.map((trip) => (
            <Link key={trip.id} to="/trips/$tripId" params={{ tripId: String(trip.id) }} className="block group">
              <Card className="overflow-hidden border-border hover:border-primary/50 transition-all hover:shadow-lg h-full flex flex-col">
                <div className="h-32 bg-gradient-to-r from-blue-600 via-indigo-600 to-violet-600 p-4 flex flex-col justify-between text-white relative">
                  <div className="flex justify-between items-start">
                    <Badge variant="secondary" className="bg-white/20 text-white backdrop-blur-md border-none text-[11px]">
                      {trip.destination?.name || "Điểm đến"}
                    </Badge>
                    <div className="flex items-center gap-1 text-[11px] bg-black/30 backdrop-blur-md px-2 py-0.5 rounded-md">
                      <Sparkles className="h-3 w-3 text-amber-300" />
                      <span>{trip.phase === "BEFORE" ? "Chưa đi" : trip.phase === "DURING" ? "Đang đi" : "Đã xong"}</span>
                    </div>
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5 text-xs text-blue-100 mb-1">
                      <MapPin className="h-3.5 w-3.5" />
                      <span>{trip.destination?.country || "Việt Nam"}</span>
                    </div>
                    <h3 className="font-bold text-lg text-white group-hover:text-blue-100 transition-colors line-clamp-1">
                      {trip.title}
                    </h3>
                  </div>
                </div>

                <CardContent className="p-4 space-y-3 flex-1 flex flex-col justify-between">
                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <div className="flex items-center gap-1.5">
                      <Calendar className="h-3.5 w-3.5" />
                      <span>{formatDateRange(trip.startDate, trip.endDate)}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Users className="h-3.5 w-3.5" />
                      <span>{trip.travelers} người</span>
                    </div>
                  </div>

                  <div className="space-y-1.5 pt-2 border-t border-border/60">
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-muted-foreground">Tiến độ lập lịch:</span>
                      <span className="font-semibold text-primary">{trip.planningProgress}%</span>
                    </div>
                    <div className="w-full bg-muted rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-primary h-full rounded-full transition-all"
                        style={{ width: `${trip.planningProgress}%` }}
                      />
                    </div>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      {/* Create Modal */}
      <CreateTripModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={() => fetchTrips()}
        initialPrompt={homePrompt}
      />
    </div>
  )
}
