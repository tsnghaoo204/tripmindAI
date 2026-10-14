import type { Activity, ActivityType } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { formatCurrency } from "@/lib/utils"
import { BedDouble, Bus, Clock, Coffee, HelpCircle, MapPin, Sparkles, Trash2, Utensils } from "lucide-react"
import { ActivityAlternatives, supportsAlternatives } from "./ActivityAlternatives"
import { getPlaceTag, isCommunityTip } from "./place-tag"

const TYPE_ICON: Record<ActivityType, React.ReactNode> = {
  FOOD: <Utensils className="h-4 w-4 text-amber-500" aria-hidden />,
  ACCOMMODATION: <BedDouble className="h-4 w-4 text-indigo-500" aria-hidden />,
  TRANSPORT: <Bus className="h-4 w-4 text-blue-500" aria-hidden />,
  REST: <Coffee className="h-4 w-4 text-slate-500" aria-hidden />,
  SIGHTSEEING: <MapPin className="h-4 w-4 text-emerald-500" aria-hidden />,
  OTHER: <MapPin className="h-4 w-4 text-emerald-500" aria-hidden />,
}

interface ActivityCardProps {
  activity: Activity
  currency: string
  deleting: boolean
  onExplain: (activityId: number) => void
  onDelete: (activityId: number) => void
  onChanged: () => void
}

export function ActivityCard({ activity, currency, deleting, onExplain, onDelete, onChanged }: ActivityCardProps) {
  const placeName = activity.place?.name || activity.placeName
  const tag = getPlaceTag(activity.place)
  const cost = activity.estimatedCost

  return (
    <Card className="hover:border-primary/40 transition-all hover:shadow-sm">
      <CardContent className="p-4 flex flex-col gap-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-xl bg-muted/80 text-foreground shrink-0 mt-0.5">
              {TYPE_ICON[activity.activityType] ?? TYPE_ICON.OTHER}
            </div>
            <div className="space-y-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-bold text-sm text-foreground">{activity.title}</span>
                {placeName && (
                  <Badge
                    variant="secondary"
                    className="text-xs bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20 flex items-center gap-1 font-semibold"
                  >
                    <MapPin className="h-3 w-3 text-emerald-600 dark:text-emerald-400" aria-hidden />
                    {placeName}
                  </Badge>
                )}
                {tag && (
                  <Badge
                    variant="outline"
                    className={`text-[11px] h-5 px-1.5 flex items-center gap-1 font-medium ${tag.color}`}
                  >
                    <span aria-hidden>{tag.icon}</span>
                    <span>{tag.label}</span>
                  </Badge>
                )}
                {activity.fromProposalId && (
                  <Badge
                    variant="outline"
                    className="text-[10px] text-primary border-primary/30 bg-primary/5 flex items-center gap-1"
                  >
                    <Sparkles className="h-2.5 w-2.5" aria-hidden /> AI Đề xuất
                  </Badge>
                )}
                {isCommunityTip(activity.place, activity.notes) && (
                  <Badge
                    variant="outline"
                    className="text-[10px] text-purple-600 dark:text-purple-400 border-purple-500/30 bg-purple-500/10 flex items-center gap-1 font-semibold"
                  >
                    <span aria-hidden>✨</span> Mẹo cộng đồng
                  </Badge>
                )}
              </div>

              <div className="flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                {(activity.startTime || activity.endTime) && (
                  <div className="flex items-center gap-1">
                    <Clock className="h-3 w-3" aria-hidden />
                    <span>
                      {activity.startTime?.slice(0, 5) || "--"} - {activity.endTime?.slice(0, 5) || "--"}
                    </span>
                  </div>
                )}
                {activity.place?.address && (
                  <div className="flex items-center gap-1 text-muted-foreground text-[11px]">
                    <span>📍 {activity.place.address}</span>
                  </div>
                )}
                {activity.place?.rating != null && (
                  <div className="flex items-center gap-1 text-amber-500 font-semibold text-[11px]">
                    <span>★ {activity.place.rating}</span>
                    {activity.place.userRatingsTotal != null && (
                      <span className="text-muted-foreground font-normal">({activity.place.userRatingsTotal})</span>
                    )}
                  </div>
                )}
                {cost != null && (
                  <Badge variant="secondary" className="text-[10px] font-medium flex items-center gap-1">
                    <span>{cost === 0 ? "Miễn phí" : formatCurrency(cost, currency)}</span>
                    {cost > 0 && activity.estimatedCostSource === "AI" && (
                      <span
                        className="text-[9px] text-primary/80 font-normal"
                        title="Ước tính AI theo giá thị trường địa phương"
                      >
                        (Dự toán AI)
                      </span>
                    )}
                    {cost > 0 && activity.estimatedCostSource === "PRICE_LEVEL" && (
                      <span className="text-[9px] text-muted-foreground font-normal" title="Ước tính từ Google Places">
                        (Google)
                      </span>
                    )}
                  </Badge>
                )}
                {activity.formattedTravelTimeToNext && (
                  <span className="text-[11px]">
                    → {activity.formattedTravelTimeToNext}
                    {activity.formattedDistanceToNext ? ` · ${activity.formattedDistanceToNext}` : ""}
                  </span>
                )}
              </div>

              {activity.notes && <p className="text-[11px] text-muted-foreground italic mt-1">{activity.notes}</p>}
            </div>
          </div>

          <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
            {activity.fromProposalId && (
              <Button
                size="icon"
                variant="ghost"
                onClick={() => onExplain(activity.id)}
                className="h-8 w-8 text-primary hover:bg-primary/10"
                title="Xem giải trình vì sao AI chọn điểm này"
                aria-label={`Giải trình AI cho ${activity.title}`}
              >
                <HelpCircle className="h-4 w-4" />
              </Button>
            )}
            <Button
              size="icon"
              variant="ghost"
              disabled={deleting}
              onClick={() => onDelete(activity.id)}
              className="h-8 w-8 text-destructive hover:bg-destructive/10"
              title="Xoá hoạt động"
              aria-label={`Xoá hoạt động ${activity.title}`}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {supportsAlternatives(activity) && (
          <ActivityAlternatives activity={activity} currency={currency} onChanged={onChanged} />
        )}
      </CardContent>
    </Card>
  )
}
