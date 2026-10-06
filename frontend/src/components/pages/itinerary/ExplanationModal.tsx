import React, { useEffect, useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { X, Sparkles, Wrench, AlertTriangle, CheckCircle, HelpCircle } from "lucide-react"
import { aiApi, type ExplanationResponse } from "@/client/api/ai-api"

interface ExplanationModalProps {
  activityId: number | null
  onClose: () => void
}

export function ExplanationModal({ activityId, onClose }: ExplanationModalProps) {
  const [data, setData] = useState<ExplanationResponse | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!activityId) return
    setLoading(true)
    aiApi
      .getExplanation(activityId)
      .then((res) => setData(res))
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [activityId])

  if (!activityId) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
      <Card className="w-full max-w-lg shadow-2xl border-border animate-in fade-in zoom-in-95">
        <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-base font-bold">Giải Trình Nguồn Gốc AI</CardTitle>
              <p className="text-xs text-muted-foreground">Vì sao AI đưa ra lựa chọn này?</p>
            </div>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} className="rounded-full">
            <X className="h-4 w-4" />
          </Button>
        </CardHeader>

        <CardContent className="p-6 space-y-4 text-xs">
          {loading ? (
            <div className="py-8 text-center text-muted-foreground">Đang truy vết nguồn gốc đề xuất...</div>
          ) : !data || !data.known ? (
            <div className="py-6 text-center space-y-2">
              <HelpCircle className="h-8 w-8 text-muted-foreground mx-auto" />
              <p className="font-semibold text-foreground">Hoạt động do người dùng tự tạo</p>
              <p className="text-muted-foreground">Hoạt động này không phát sinh từ đề xuất của AI nên không có dữ liệu giải trình.</p>
            </div>
          ) : (
            <>
              {/* Proposal Reason */}
              <div className="p-3 rounded-xl bg-primary/5 border border-primary/20 space-y-1.5">
                <div className="font-semibold text-primary flex items-center gap-1.5">
                  <CheckCircle className="h-4 w-4" /> Lý do đề xuất:
                </div>
                <p className="text-foreground leading-relaxed">
                  {data.changeReason || data.proposalReason || data.proposalSummary}
                </p>
              </div>

              {/* Tools Executed */}
              {data.tools && data.tools.length > 0 && (
                <div className="space-y-2">
                  <span className="font-semibold text-muted-foreground uppercase text-[11px] tracking-wider">
                    Các công cụ AI đã thực thi
                  </span>
                  <div className="space-y-1.5">
                    {data.tools.map((t, idx) => (
                      <div key={idx} className="p-2.5 rounded-lg bg-muted/60 border border-border flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Wrench className="h-3.5 w-3.5 text-primary" />
                          <span className="font-medium text-foreground">{t.toolName}</span>
                        </div>
                        <Badge variant="outline" className="text-[10px]">
                          {t.executionTimeMs} ms
                        </Badge>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Rejected Candidates */}
              {data.rejected && data.rejected.length > 0 && (
                <div className="space-y-2 pt-2 border-t">
                  <span className="font-semibold text-destructive uppercase text-[11px] tracking-wider flex items-center gap-1">
                    <AlertTriangle className="h-3.5 w-3.5" /> Ứng viên bị loại & lý do
                  </span>
                  <div className="space-y-1">
                    {data.rejected.map((r, idx) => (
                      <div key={idx} className="p-2 rounded bg-destructive/10 text-destructive text-[11px] flex justify-between">
                        <span>{r.title}</span>
                        <span className="font-medium">{r.reason}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}

          <div className="pt-2 flex justify-end">
            <Button size="sm" onClick={onClose}>Đóng</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
