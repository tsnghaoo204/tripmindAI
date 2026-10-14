import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { formatCurrency } from "@/lib/utils"
import { CheckCircle2, Clock, TrendingDown, TrendingUp, XCircle } from "lucide-react"
import type { ChatProposal } from "./chat-state"

const STATUS_LABEL: Record<ChatProposal["status"], string> = {
  PENDING: "Chờ xác nhận",
  APPLIED: "Đã áp dụng",
  REJECTED: "Đã từ chối",
  EXPIRED: "Đã hết hạn",
  REVERTED: "Đã hoàn tác",
}

interface ProposalCardProps {
  proposal: ChatProposal
  busy: boolean
  onApply: (proposalId: number) => void
  onReject: (proposalId: number) => void
}

export function ProposalCard({ proposal, busy, onApply, onReject }: ProposalCardProps) {
  const { estimatedCostDelta: cost, travelTimeDelta: travel } = proposal
  return (
    <Card className="border-primary/30 shadow-md bg-card/90 overflow-hidden">
      <div className="bg-primary/10 px-3 py-1.5 text-xs font-semibold text-primary flex items-center justify-between gap-2">
        <span>Đề xuất thay đổi lịch trình</span>
        <div className="flex items-center gap-1">
          <Badge variant="outline" className="text-[10px] bg-background">
            {proposal.changeCount} thao tác
          </Badge>
          {proposal.status !== "PENDING" && (
            <Badge variant="secondary" className="text-[10px]">
              {STATUS_LABEL[proposal.status]}
            </Badge>
          )}
        </div>
      </div>
      <CardContent className="p-3 space-y-2 text-xs">
        <p className="font-medium text-foreground">{proposal.summary}</p>

        <div className="flex items-center gap-3 text-[11px] text-muted-foreground pt-1">
          {cost != null && (
            <div className="flex items-center gap-1">
              {cost >= 0 ? (
                <TrendingUp className="h-3.5 w-3.5 text-rose-500" aria-hidden />
              ) : (
                <TrendingDown className="h-3.5 w-3.5 text-emerald-500" aria-hidden />
              )}
              <span>
                {cost >= 0 ? "+" : ""}
                {formatCurrency(cost)}
              </span>
            </div>
          )}
          {travel != null && travel !== 0 && (
            <div className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5 text-primary" aria-hidden />
              <span>
                {travel > 0 ? "+" : ""}
                {travel} phút di chuyển
              </span>
            </div>
          )}
        </div>

        {proposal.status === "PENDING" && (
          <div className="flex gap-2 pt-2 border-t">
            <Button
              size="sm"
              disabled={busy}
              className="flex-1 h-8 text-xs gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={() => onApply(proposal.proposalId)}
            >
              <CheckCircle2 className="h-3.5 w-3.5" /> Áp dụng
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={busy}
              className="h-8 text-xs gap-1.5 text-destructive hover:bg-destructive/10"
              onClick={() => onReject(proposal.proposalId)}
            >
              <XCircle className="h-3.5 w-3.5" /> Từ chối
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
