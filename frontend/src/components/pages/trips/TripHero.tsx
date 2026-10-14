import type { Trip, TripPhase } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { formatCurrency, formatDateRange } from "@/lib/utils"
import { Calendar, Copy, DollarSign, Sparkles, ThumbsUp, Trash2, Users } from "lucide-react"

export const PHASE_LABEL: Record<TripPhase, string> = {
  BEFORE: "Chưa đi",
  DURING: "Đang đi",
  AFTER: "Đã xong",
}

interface TripHeroProps {
  trip: Trip
  canGenerate: boolean
  isGenerating: boolean
  onGenerate: () => void
  onChangePhase: (phase: TripPhase) => void
  onReview: () => void
  onDuplicate: () => void
  onDelete: () => void
}

export function TripHero({
  trip,
  canGenerate,
  isGenerating,
  onGenerate,
  onChangePhase,
  onReview,
  onDuplicate,
  onDelete,
}: TripHeroProps) {
  return (
    <div className="rounded-3xl bg-gradient-to-r from-blue-700 via-indigo-600 to-violet-700 p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
      <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-3 max-w-2xl">
          <div className="flex flex-wrap items-center gap-2">
            <Badge className="bg-white/20 text-white backdrop-blur-md border-none">
              {trip.destination.name}, {trip.destination.country}
            </Badge>
            <fieldset className="flex items-center gap-1 bg-black/30 p-1 rounded-xl backdrop-blur-md">
              <legend className="sr-only">Giai đoạn chuyến đi</legend>
              {(["BEFORE", "DURING", "AFTER"] as const).map((p) => (
                <button
                  type="button"
                  key={p}
                  aria-pressed={trip.phase === p}
                  onClick={() => onChangePhase(p)}
                  className={`px-2 py-0.5 rounded-lg text-[11px] font-semibold transition-all cursor-pointer ${
                    trip.phase === p ? "bg-white text-slate-900 shadow-sm" : "text-white/80 hover:bg-white/10"
                  }`}
                >
                  {PHASE_LABEL[p]}
                </button>
              ))}
            </fieldset>
            {trip.travelStyle && (
              <Badge className="bg-white/10 text-white backdrop-blur-md border-none">
                Phong cách: {trip.travelStyle}
              </Badge>
            )}
          </div>

          <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight">{trip.name}</h1>

          <div className="flex flex-wrap items-center gap-4 text-xs sm:text-sm text-blue-100">
            <div className="flex items-center gap-1.5">
              <Calendar className="h-4 w-4" aria-hidden />
              <span>{formatDateRange(trip.startDate, trip.endDate)}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Users className="h-4 w-4" aria-hidden />
              <span>{trip.travelers} thành viên</span>
            </div>
            {trip.budget != null && (
              <div className="flex items-center gap-1.5">
                <DollarSign className="h-4 w-4" aria-hidden />
                <span>Ngân sách: {formatCurrency(trip.budget, trip.currency)}</span>
              </div>
            )}
          </div>
        </div>

        <div className="flex flex-wrap md:flex-col gap-2 shrink-0">
          {canGenerate && (
            <Button
              onClick={onGenerate}
              disabled={isGenerating}
              className="bg-amber-400 hover:bg-amber-500 text-slate-900 font-bold gap-2 shadow-lg"
            >
              <Sparkles className="h-4 w-4" />
              {isGenerating ? "AI đang lên lịch..." : "Sinh lịch trình tự động"}
            </Button>
          )}

          {trip.phase === "AFTER" && (
            <Button
              size="sm"
              onClick={onReview}
              className="bg-amber-400 hover:bg-amber-500 text-slate-900 font-semibold gap-1.5 shadow"
            >
              <ThumbsUp className="h-3.5 w-3.5" /> Đánh giá địa điểm
            </Button>
          )}

          <div className="flex gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={onDuplicate}
              className="bg-white/20 hover:bg-white/30 text-white border-none gap-1.5"
            >
              <Copy className="h-3.5 w-3.5" /> Nhân bản
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={onDelete}
              className="bg-white/10 hover:bg-rose-500/80 text-white border-none gap-1.5"
            >
              <Trash2 className="h-3.5 w-3.5" /> Xoá
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
