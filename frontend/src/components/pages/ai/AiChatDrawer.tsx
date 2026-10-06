import React, { useState, useEffect, useRef } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  Sparkles,
  Send,
  X,
  Bot,
  User as UserIcon,
  CheckCircle2,
  XCircle,
  RotateCcw,
  Wrench,
  Clock,
  ArrowRight,
  TrendingDown,
  TrendingUp,
} from "lucide-react"
import { toast } from "sonner"
import { aiApi, type ProposalDetail } from "@/client/api/ai-api"
import { formatCurrency } from "@/lib/utils"

interface AiChatDrawerProps {
  tripId: number
  isOpen: boolean
  onClose: () => void
  onProposalApplied?: () => void
}

interface MessageItem {
  id: string
  role: "user" | "assistant"
  text: string
  toolCall?: string
  proposal?: any
}

export function AiChatDrawer({
  tripId,
  isOpen,
  onClose,
  onProposalApplied,
}: AiChatDrawerProps) {
  const [messages, setMessages] = useState<MessageItem[]>([
    {
      id: "welcome",
      role: "assistant",
      text: "Xin chào! Mình là Trợ lý AI TripMind. Bạn muốn mình gợi ý thêm địa điểm, sắp xếp thứ tự tham quan hay phân tích ngân sách cho chuyến đi?",
    },
  ])
  const [input, setInput] = useState("")
  const [isStreaming, setIsStreaming] = useState(false)
  const [currentTool, setCurrentTool] = useState<string | null>(null)
  const [appliedProposalId, setAppliedProposalId] = useState<number | null>(null)
  const [undoExpiresAt, setUndoExpiresAt] = useState<number | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages, currentTool])

  if (!isOpen) return null

  const handleSend = () => {
    if (!input.trim() || isStreaming) return

    const userText = input.trim()
    setInput("")
    setMessages((prev) => [...prev, { id: String(Date.now()), role: "user", text: userText }])
    setIsStreaming(true)
    setCurrentTool(null)

    let assistantText = ""
    let activeProposal: any = null

    aiApi.streamChat(
      tripId,
      { message: userText },
      {
        onToken: (token) => {
          assistantText += token
          setMessages((prev) => {
            const last = prev[prev.length - 1]
            if (last && last.role === "assistant" && last.id === "streaming") {
              return [...prev.slice(0, -1), { ...last, text: assistantText }]
            }
            return [
              ...prev,
              { id: "streaming", role: "assistant", text: assistantText },
            ]
          })
        },
        onToolStart: (tool) => {
          setCurrentTool(tool)
        },
        onToolEnd: () => {
          setCurrentTool(null)
        },
        onProposal: (proposal) => {
          activeProposal = proposal
          setMessages((prev) => {
            const last = prev[prev.length - 1]
            if (last && last.role === "assistant") {
              return [...prev.slice(0, -1), { ...last, proposal }]
            }
            return prev
          })
        },
        onDone: () => {
          setIsStreaming(false)
          setCurrentTool(null)
          setMessages((prev) =>
            prev.map((m) => (m.id === "streaming" ? { ...m, id: String(Date.now()), proposal: activeProposal } : m)),
          )
        },
        onError: (err) => {
          setIsStreaming(false)
          setCurrentTool(null)
          toast.error(err.message || "Lỗi giao tiếp với Trợ lý AI")
        },
      },
    )
  }

  const handleApplyProposal = async (proposalId: number) => {
    try {
      await aiApi.applyProposal(tripId, proposalId)
      toast.success("Đã áp dụng đề xuất vào lịch trình thành công!")
      setAppliedProposalId(proposalId)
      setUndoExpiresAt(Date.now() + 10 * 60 * 1000) // 10 minutes window
      onProposalApplied?.()
    } catch (err: any) {
      toast.error(err.message || "Không thể áp dụng đề xuất")
    }
  }

  const handleRejectProposal = async (proposalId: number) => {
    try {
      await aiApi.rejectProposal(proposalId)
      toast.info("Đã từ chối đề xuất.")
      setMessages((prev) =>
        prev.map((m) =>
          m.proposal?.id === proposalId
            ? { ...m, proposal: { ...m.proposal, status: "REJECTED" } }
            : m,
        ),
      )
    } catch (err: any) {
      toast.error(err.message || "Không thể từ chối đề xuất")
    }
  }

  const handleUndo = async () => {
    if (!appliedProposalId) return
    try {
      await aiApi.undoProposal(tripId, appliedProposalId)
      toast.success("Đã hoàn tác các thay đổi do AI đề xuất!")
      setAppliedProposalId(null)
      onProposalApplied?.()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi hoàn tác")
    }
  }

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full sm:w-[460px] bg-background/95 backdrop-blur-xl border-l border-border shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
      {/* Header */}
      <div className="p-4 border-b border-border flex items-center justify-between bg-card/60">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 text-white shadow-md shadow-blue-500/20">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h3 className="font-bold text-sm leading-tight flex items-center gap-1.5">
              Trợ lý AI TripMind
              <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4 border-emerald-500/30 text-emerald-500 bg-emerald-500/10">
                Gemini ReAct
              </Badge>
            </h3>
            <p className="text-xs text-muted-foreground">Phân tích dữ liệu & Đề xuất lịch trình</p>
          </div>
        </div>
        <Button variant="ghost" size="icon" onClick={onClose} className="rounded-full">
          <X className="h-4 w-4" />
        </Button>
      </div>

      {/* Undo Banner if active */}
      {appliedProposalId && (
        <div className="bg-amber-500/10 border-b border-amber-500/20 px-4 py-2 flex items-center justify-between text-xs text-amber-600 dark:text-amber-400">
          <div className="flex items-center gap-1.5">
            <Clock className="h-3.5 w-3.5" />
            <span>Có thể hoàn tác đề xuất trong 10 phút</span>
          </div>
          <Button size="sm" variant="outline" onClick={handleUndo} className="h-7 text-xs gap-1 border-amber-500/30 text-amber-600 hover:bg-amber-500/10">
            <RotateCcw className="h-3 w-3" /> Hoàn tác
          </Button>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((m) => (
          <div key={m.id} className={`flex gap-3 ${m.role === "user" ? "justify-end" : "justify-start"}`}>
            {m.role === "assistant" && (
              <div className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center shrink-0">
                <Bot className="h-4 w-4" />
              </div>
            )}
            <div className={`max-w-[85%] space-y-2 ${m.role === "user" ? "items-end" : "items-start"}`}>
              <div
                className={`p-3 rounded-2xl text-sm leading-relaxed ${
                  m.role === "user"
                    ? "bg-primary text-primary-foreground rounded-br-xs"
                    : "bg-muted/70 text-foreground rounded-bl-xs border border-border/50"
                }`}
              >
                {m.text}
              </div>

              {/* Proposal Card if exists */}
              {m.proposal && (
                <Card className="border-primary/30 shadow-md bg-card/90 overflow-hidden">
                  <div className="bg-primary/10 px-3 py-1.5 text-xs font-semibold text-primary flex items-center justify-between">
                    <span>Đề xuất thay đổi lịch trình</span>
                    <Badge variant="outline" className="text-[10px] bg-background">
                      {m.proposal.changes?.length || 1} thao tác
                    </Badge>
                  </div>
                  <CardContent className="p-3 space-y-2 text-xs">
                    <p className="font-medium text-foreground">{m.proposal.summary}</p>

                    {/* Cost & Travel time deltas */}
                    <div className="flex items-center gap-3 text-[11px] text-muted-foreground pt-1">
                      {m.proposal.estimatedCostDelta != null && (
                        <div className="flex items-center gap-1">
                          {m.proposal.estimatedCostDelta >= 0 ? (
                            <TrendingUp className="h-3.5 w-3.5 text-rose-500" />
                          ) : (
                            <TrendingDown className="h-3.5 w-3.5 text-emerald-500" />
                          )}
                          <span>{formatCurrency(m.proposal.estimatedCostDelta)}</span>
                        </div>
                      )}
                      {m.proposal.travelTimeDeltaMinutes != null && (
                        <div className="flex items-center gap-1">
                          <Clock className="h-3.5 w-3.5 text-primary" />
                          <span>{m.proposal.travelTimeDeltaMinutes} phút di chuyển</span>
                        </div>
                      )}
                    </div>

                    {m.proposal.status === "PENDING" && (
                      <div className="flex gap-2 pt-2 border-t">
                        <Button
                          size="sm"
                          className="flex-1 h-8 text-xs gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
                          onClick={() => handleApplyProposal(m.proposal.proposalId || m.proposal.id)}
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" /> Áp dụng
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-8 text-xs gap-1.5 text-destructive hover:bg-destructive/10"
                          onClick={() => handleRejectProposal(m.proposal.proposalId || m.proposal.id)}
                        >
                          <XCircle className="h-3.5 w-3.5" /> Từ chối
                        </Button>
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}
            </div>
            {m.role === "user" && (
              <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center shrink-0">
                <UserIcon className="h-4 w-4" />
              </div>
            )}
          </div>
        ))}

        {/* Tool Call indicator */}
        {currentTool && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted/40 p-2 rounded-lg border border-border/60 animate-pulse">
            <Wrench className="h-3.5 w-3.5 text-primary" />
            <span>AI đang chạy công cụ: <strong>{currentTool}</strong>...</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Quick Prompts */}
      <div className="px-3 py-2 border-t border-border flex gap-1.5 overflow-x-auto text-[11px] no-scrollbar">
        {[
          "Tối ưu lại thứ tự ngày 1",
          "Gợi ý quán ăn trưa",
          "Dự báo thời tiết chuyến đi",
          "Kiểm tra ngân sách",
        ].map((q) => (
          <button
            key={q}
            onClick={() => {
              setInput(q)
            }}
            className="px-2.5 py-1 rounded-full bg-muted hover:bg-accent text-muted-foreground hover:text-foreground whitespace-nowrap transition-colors cursor-pointer"
          >
            {q}
          </button>
        ))}
      </div>

      {/* Input */}
      <div className="p-3 border-t border-border bg-card">
        <form
          onSubmit={(e) => {
            e.preventDefault()
            handleSend()
          }}
          className="flex gap-2"
        >
          <Input
            placeholder="Hỏi AI bất kỳ điều gì về chuyến đi..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={isStreaming}
            className="text-sm"
          />
          <Button type="submit" size="icon" disabled={isStreaming || !input.trim()}>
            <Send className="h-4 w-4" />
          </Button>
        </form>
      </div>
    </div>
  )
}
