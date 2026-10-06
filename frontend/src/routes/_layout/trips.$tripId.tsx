import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState, useEffect } from "react"
import { tripApi } from "@/client/api/trip-api"
import { itineraryApi, type ItineraryResponse } from "@/client/api/itinerary-api"
import { weatherApi } from "@/client/api/weather-api"
import { aiApi, type GenerateJobStatus } from "@/client/api/ai-api"
import type { Trip, TripWeather } from "@/client/types"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  Calendar,
  MapPin,
  Users,
  Sparkles,
  Bot,
  Copy,
  Trash2,
  DollarSign,
  Luggage,
  CheckCircle,
  Clock,
  RotateCw,
} from "lucide-react"
import { DayTimeline } from "@/components/pages/itinerary/DayTimeline"
import { BudgetView } from "@/components/pages/budget/BudgetView"
import { ChecklistView } from "@/components/pages/checklist/ChecklistView"
import { AiChatDrawer } from "@/components/pages/ai/AiChatDrawer"
import { formatDateRange, formatCurrency } from "@/lib/utils"
import { toast } from "sonner"

export const Route = createFileRoute("/_layout/trips/$tripId")({
  component: TripDetailPage,
})

function TripDetailPage() {
  const { tripId: tripIdParam } = Route.useParams()
  const tripId = Number(tripIdParam)
  const navigate = useNavigate()

  const [trip, setTrip] = useState<Trip | null>(null)
  const [itinerary, setItinerary] = useState<ItineraryResponse | null>(null)
  const [weather, setWeather] = useState<TripWeather | undefined>()
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"itinerary" | "budget" | "checklist">("itinerary")

  // AI Drawer State
  const [isAiOpen, setIsAiOpen] = useState(false)

  // AI Generation State
  const [isGenerating, setIsGenerating] = useState(false)
  const [generateJob, setGenerateJob] = useState<GenerateJobStatus | null>(null)

  const loadData = async () => {
    try {
      setLoading(true)
      const [tripData, itinData, weatherData] = await Promise.all([
        tripApi.getTripById(tripId),
        itineraryApi.getItinerary(tripId),
        weatherApi.getTripWeather(tripId).catch(() => undefined),
      ])
      setTrip(tripData)
      setItinerary(itinData)
      setWeather(weatherData)
    } catch {
      toast.error("Không thể tải thông tin chuyến đi")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [tripId])

  const handleDuplicateTrip = async () => {
    try {
      const res: any = await tripApi.createTrip({ ...trip, title: `${trip?.title} (Bản sao)` })
      toast.success("Đã nhân bản chuyến đi!")
      navigate({ to: "/" })
    } catch {
      toast.error("Không thể nhân bản chuyến đi")
    }
  }

  const handleDeleteTrip = async () => {
    if (!confirm("Bạn có chắc chắn muốn xoá toàn bộ chuyến đi này?")) return
    try {
      await tripApi.deleteTrip(tripId)
      toast.success("Đã xoá chuyến đi")
      navigate({ to: "/" })
    } catch {
      toast.error("Không thể xoá chuyến đi")
    }
  }

  // Auto Generate Itinerary flow
  const handleStartGenerate = async () => {
    try {
      setIsGenerating(true)
      const job = await aiApi.startGenerateItinerary(tripId)
      setGenerateJob(job)
      toast.info("Đã bắt đầu tác vụ sinh lịch trình...")

      // Polling loop
      const interval = setInterval(async () => {
        try {
          const status = await aiApi.getGenerateStatus(tripId, job.jobId)
          setGenerateJob(status)
          if (status.state === "DONE") {
            clearInterval(interval)
            setIsGenerating(false)
            toast.success("Sinh lịch trình thành công!")
            loadData()
          } else if (status.state === "FAILED") {
            clearInterval(interval)
            setIsGenerating(false)
            toast.error(status.error || "Không thể sinh lịch trình")
          }
        } catch {
          clearInterval(interval)
          setIsGenerating(false)
        }
      }, 2000)
    } catch (err: any) {
      setIsGenerating(false)
      toast.error(err.message || "Lỗi khi kích hoạt AI sinh lịch trình")
    }
  }

  if (loading && !trip) {
    return <div className="py-20 text-center text-sm text-muted-foreground">Đang tải chi tiết chuyến đi...</div>
  }

  if (!trip) {
    return <div className="py-20 text-center text-sm text-muted-foreground">Không tìm thấy chuyến đi.</div>
  }

  return (
    <div className="container mx-auto px-4 sm:px-6 py-6 space-y-6 pb-24">
      {/* Trip Hero Header */}
      <div className="rounded-3xl bg-gradient-to-r from-blue-700 via-indigo-600 to-violet-700 p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-3 max-w-2xl">
            <div className="flex flex-wrap items-center gap-2">
              <Badge className="bg-white/20 text-white backdrop-blur-md border-none">
                {trip.destination.name}, {trip.destination.country}
              </Badge>
              <Badge className="bg-black/30 text-white backdrop-blur-md border-none">
                {trip.phase === "BEFORE" ? "Chưa đi" : trip.phase === "DURING" ? "Đang đi" : "Đã xong"}
              </Badge>
              {trip.travelStyle && (
                <Badge className="bg-white/10 text-white backdrop-blur-md border-none">
                  Phong cách: {trip.travelStyle}
                </Badge>
              )}
            </div>

            <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight">{trip.title}</h1>

            <div className="flex flex-wrap items-center gap-4 text-xs sm:text-sm text-blue-100">
              <div className="flex items-center gap-1.5">
                <Calendar className="h-4 w-4" />
                <span>{formatDateRange(trip.startDate, trip.endDate)}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Users className="h-4 w-4" />
                <span>{trip.travelers} thành viên</span>
              </div>
              {trip.budget != null && (
                <div className="flex items-center gap-1.5">
                  <DollarSign className="h-4 w-4" />
                  <span>Ngân sách: {formatCurrency(trip.budget, trip.currency)}</span>
                </div>
              )}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="flex flex-wrap md:flex-col gap-2 shrink-0">
            {(!itinerary || itinerary.days.every((d) => d.activities.length === 0)) && (
              <Button
                onClick={handleStartGenerate}
                disabled={isGenerating}
                className="bg-amber-400 hover:bg-amber-500 text-slate-900 font-bold gap-2 shadow-lg"
              >
                <Sparkles className="h-4 w-4" />
                {isGenerating ? "AI đang lên lịch..." : "Sinh lịch trình tự động"}
              </Button>
            )}

            <div className="flex gap-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={handleDuplicateTrip}
                className="bg-white/20 hover:bg-white/30 text-white border-none gap-1.5"
                title="Nhân bản chuyến"
              >
                <Copy className="h-3.5 w-3.5" /> Nhân bản
              </Button>
              <Button
                variant="secondary"
                size="sm"
                onClick={handleDeleteTrip}
                className="bg-white/10 hover:bg-rose-500/80 text-white border-none gap-1.5"
                title="Xoá chuyến"
              >
                <Trash2 className="h-3.5 w-3.5" /> Xoá
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Generation Progress Indicator if running */}
      {isGenerating && generateJob && (
        <Card className="border-primary/40 bg-primary/5 p-4 animate-pulse">
          <div className="flex items-center justify-between text-xs font-semibold text-primary mb-2">
            <span className="flex items-center gap-2">
              <RotateCw className="h-4 w-4 animate-spin" />
              {generateJob.step}
            </span>
            <span>{Math.round(generateJob.progress * 100)}%</span>
          </div>
          <div className="w-full bg-muted rounded-full h-2 overflow-hidden">
            <div
              className="bg-primary h-full transition-all duration-300"
              style={{ width: `${Math.round(generateJob.progress * 100)}%` }}
            />
          </div>
        </Card>
      )}

      {/* Navigation Tabs */}
      <div className="flex border-b border-border text-sm font-medium">
        <button
          onClick={() => setActiveTab("itinerary")}
          className={`py-3 px-5 border-b-2 font-semibold flex items-center gap-2 transition-colors cursor-pointer ${
            activeTab === "itinerary"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Calendar className="h-4 w-4" /> Lịch Trình Chi Tiết
        </button>
        <button
          onClick={() => setActiveTab("budget")}
          className={`py-3 px-5 border-b-2 font-semibold flex items-center gap-2 transition-colors cursor-pointer ${
            activeTab === "budget"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <DollarSign className="h-4 w-4" /> Ngân Sách & Chi Tiêu
        </button>
        <button
          onClick={() => setActiveTab("checklist")}
          className={`py-3 px-5 border-b-2 font-semibold flex items-center gap-2 transition-colors cursor-pointer ${
            activeTab === "checklist"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Luggage className="h-4 w-4" /> Checklist Chuẩn Bị
        </button>
      </div>

      {/* Tab Contents */}
      {activeTab === "itinerary" && (
        <DayTimeline
          tripId={tripId}
          days={itinerary?.days || []}
          weather={weather}
          onRefresh={loadData}
          onOpenAiChat={() => setIsAiOpen(true)}
        />
      )}

      {activeTab === "budget" && <BudgetView tripId={tripId} />}

      {activeTab === "checklist" && <ChecklistView tripId={tripId} />}

      {/* Floating AI Chat Trigger Button */}
      <button
        onClick={() => setIsAiOpen(true)}
        className="fixed bottom-6 right-6 z-40 flex items-center gap-2.5 px-4 py-3 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-semibold text-sm shadow-xl shadow-blue-500/30 hover:scale-105 transition-all cursor-pointer group"
      >
        <div className="p-1 rounded-full bg-white/20">
          <Sparkles className="h-4 w-4 text-amber-300 animate-pulse" />
        </div>
        <span>Trợ lý AI TripMind</span>
      </button>

      {/* AI Chat Drawer */}
      <AiChatDrawer
        tripId={tripId}
        isOpen={isAiOpen}
        onClose={() => setIsAiOpen(false)}
        onProposalApplied={loadData}
      />
    </div>
  )
}
