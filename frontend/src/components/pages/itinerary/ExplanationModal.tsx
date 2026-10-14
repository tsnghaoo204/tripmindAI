import { aiApi } from "@/client/api/ai-api"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { queryKeys } from "@/hooks/query-keys"
import { useQuery } from "@tanstack/react-query"
import { AlertTriangle, CheckCircle, HelpCircle, Sparkles, Wrench } from "lucide-react"

interface ExplanationModalProps {
  activityId: number | null
  onClose: () => void
}

interface RejectedCandidate {
  title?: string
  reason?: string
}

function rejectedList(value: unknown): RejectedCandidate[] {
  return Array.isArray(value) ? value.filter((r): r is RejectedCandidate => !!r && typeof r === "object") : []
}

export function ExplanationModal({ activityId, onClose }: ExplanationModalProps) {
  const { data, isLoading } = useQuery({
    queryKey: queryKeys.explanation(activityId ?? 0),
    queryFn: () => aiApi.getExplanation(activityId as number),
    enabled: activityId != null,
  })
  const rejected = rejectedList(data?.rejected)

  return (
    <Dialog open={activityId != null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <div className="p-2 rounded-lg bg-primary/10 text-primary">
            <Sparkles className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle>Giải Trình Nguồn Gốc AI</DialogTitle>
            <DialogDescription>Vì sao AI đưa ra lựa chọn này?</DialogDescription>
          </div>
        </DialogHeader>

        <DialogBody className="space-y-4 text-xs">
          {isLoading ? (
            <div className="py-8 text-center text-muted-foreground">Đang truy vết nguồn gốc đề xuất...</div>
          ) : !data || !data.known ? (
            <div className="py-6 text-center space-y-2">
              <HelpCircle className="h-8 w-8 text-muted-foreground mx-auto" aria-hidden />
              <p className="font-semibold text-foreground">Không có dữ liệu giải trình</p>
              <p className="text-muted-foreground">
                {data?.unknownReason ||
                  "Hoạt động này không phát sinh từ đề xuất của AI nên không có dữ liệu giải trình."}
              </p>
            </div>
          ) : (
            <>
              <div className="p-3 rounded-xl bg-primary/5 border border-primary/20 space-y-1.5">
                <div className="font-semibold text-primary flex items-center gap-1.5">
                  <CheckCircle className="h-4 w-4" aria-hidden /> Lý do đề xuất:
                </div>
                <p className="text-foreground leading-relaxed">
                  {data.changeReason || data.proposalReason || data.proposalSummary}
                </p>
              </div>

              {data.tools && data.tools.length > 0 && (
                <div className="space-y-2">
                  <span className="font-semibold text-muted-foreground uppercase text-[11px] tracking-wider">
                    Các công cụ AI đã thực thi
                  </span>
                  <ul className="space-y-1.5">
                    {data.tools.map((t) => (
                      <li
                        key={t.id}
                        className="p-2.5 rounded-lg bg-muted/60 border border-border flex items-center justify-between"
                      >
                        <div className="flex items-center gap-2">
                          <Wrench className="h-3.5 w-3.5 text-primary" aria-hidden />
                          <span className="font-medium text-foreground">{t.toolName}</span>
                        </div>
                        <Badge variant="outline" className="text-[10px]">
                          {t.executionTimeMs ?? "--"} ms
                        </Badge>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {rejected.length > 0 && (
                <div className="space-y-2 pt-2 border-t">
                  <span className="font-semibold text-destructive uppercase text-[11px] tracking-wider flex items-center gap-1">
                    <AlertTriangle className="h-3.5 w-3.5" aria-hidden /> Ứng viên bị loại & lý do
                  </span>
                  <ul className="space-y-1">
                    {rejected.map((r, idx) => (
                      <li
                        key={`${r.title}-${idx}`}
                        className="p-2 rounded bg-destructive/10 text-destructive text-[11px] flex justify-between gap-2"
                      >
                        <span>{r.title}</span>
                        <span className="font-medium">{r.reason}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          )}
        </DialogBody>

        <DialogFooter>
          <DialogClose asChild>
            <Button size="sm">Đóng</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
