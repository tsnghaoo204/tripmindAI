import { itineraryApi } from "@/client/api/itinerary-api"
import { getErrorMessage } from "@/client/errors"
import type { Activity } from "@/client/types"
import { formatCurrency } from "@/lib/utils"
import { useMutation } from "@tanstack/react-query"
import { Sparkles } from "lucide-react"
import { toast } from "sonner"

const TIER_CLASS: Record<string, string> = {
  BUDGET: "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400",
  PREMIUM: "bg-purple-500/15 text-purple-600 dark:text-purple-400",
  BALANCED: "bg-blue-500/15 text-blue-600 dark:text-blue-400",
}

/** Activity types that get budget / balanced / premium options. */
export function supportsAlternatives(activity: Activity): boolean {
  return (
    activity.activityType === "FOOD" ||
    activity.activityType === "ACCOMMODATION" ||
    (activity.alternatives?.length ?? 0) > 0
  )
}

interface ActivityAlternativesProps {
  activity: Activity
  currency: string
  onChanged: () => void
}

export function ActivityAlternatives({ activity, currency, onChanged }: ActivityAlternativesProps) {
  const alternatives = activity.alternatives ?? []

  const loadMutation = useMutation({
    mutationFn: () => itineraryApi.getAlternatives(activity.id),
    onSuccess: () => {
      toast.success("Đã tìm phương án lựa chọn theo ngân sách!")
      onChanged()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể tải phương án lựa chọn")),
  })

  const selectMutation = useMutation({
    mutationFn: (altId: number) => itineraryApi.selectAlternative(activity.id, altId),
    onSuccess: (updated) => {
      toast.success(`Đã đổi sang: ${updated.title}`)
      onChanged()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể chuyển phương án")),
  })

  return (
    <div className="mt-2 pt-2.5 border-t border-border/60 w-full">
      <div className="flex items-center justify-between gap-2 mb-2">
        <div className="flex items-center gap-1.5 text-[11px] font-semibold text-muted-foreground">
          <Sparkles className="h-3 w-3 text-amber-500" aria-hidden />
          <span>Lựa chọn theo ngân sách ({activity.activityType === "ACCOMMODATION" ? "Lưu trú" : "Ăn uống"}):</span>
        </div>
        {alternatives.length === 0 && (
          <button
            type="button"
            onClick={() => loadMutation.mutate()}
            disabled={loadMutation.isPending}
            className="text-[11px] text-primary hover:underline font-semibold cursor-pointer flex items-center gap-1 disabled:opacity-60"
          >
            <span>💡 {loadMutation.isPending ? "Đang tìm lựa chọn..." : "Gợi ý lựa chọn theo ngân sách"}</span>
          </button>
        )}
      </div>

      {alternatives.length > 0 && (
        <div className={`grid grid-cols-1 gap-2 ${alternatives.length >= 3 ? "sm:grid-cols-3" : "sm:grid-cols-2"}`}>
          {alternatives.map((alt) => (
            <button
              type="button"
              key={alt.id}
              aria-pressed={alt.isSelected}
              onClick={() => selectMutation.mutate(alt.id)}
              disabled={selectMutation.isPending}
              className={`p-2.5 rounded-xl border text-left transition-all cursor-pointer relative flex flex-col justify-between ${
                alt.isSelected
                  ? "bg-primary/10 border-primary shadow-sm ring-1 ring-primary/40"
                  : "bg-muted/30 hover:bg-muted/70 border-border/80 hover:border-border"
              }`}
            >
              <div>
                <div className="flex items-center justify-between gap-1 mb-1">
                  <span
                    className={`text-[10px] font-bold px-1.5 py-0.5 rounded-md ${TIER_CLASS[alt.tier] ?? TIER_CLASS.BALANCED}`}
                  >
                    {alt.tierLabel || alt.tier}
                  </span>
                  {alt.isSelected && (
                    <span className="text-[10px] font-bold text-primary flex items-center gap-0.5">✓ Đang chọn</span>
                  )}
                </div>
                <p className="text-xs font-semibold text-foreground line-clamp-1">{alt.placeName || alt.title}</p>
              </div>
              <div className="mt-2 flex items-center justify-between text-[11px]">
                <span className="font-bold text-foreground">
                  {alt.estimatedCost == null
                    ? "Chưa rõ giá"
                    : alt.estimatedCost === 0
                      ? "Miễn phí"
                      : formatCurrency(alt.estimatedCost, currency)}
                </span>
                {alt.rating != null && (
                  <span className="text-amber-500 font-medium text-[10px]">
                    ★ {alt.rating}
                    {alt.userRatingsTotal != null && ` (${alt.userRatingsTotal})`}
                  </span>
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
