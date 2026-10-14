import { aiApi } from "@/client/api/ai-api"
import { getErrorMessage, skipReasonLabel } from "@/client/errors"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogDescription, DialogTitle, SheetContent } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useQuery } from "@tanstack/react-query"
import { Bot, Clock, RotateCcw, Send, Sparkles, User as UserIcon, Wrench, X } from "lucide-react"
import { useCallback, useEffect, useRef, useState } from "react"
import { toast } from "sonner"
import { ProposalCard } from "./ProposalCard"
import {
  type ChatMessageItem,
  addProposal,
  appendToken,
  finishStreaming,
  messagesFromHistory,
  newMessageId,
  proposalFromEvent,
  setProposalStatus,
} from "./chat-state"

interface AiChatDrawerProps {
  tripId: number
  isOpen: boolean
  onClose: () => void
  onProposalApplied?: () => void
}

const WELCOME: ChatMessageItem = {
  id: "welcome",
  role: "assistant",
  text: "Xin chào! Mình là Trợ lý AI TripMind. Bạn muốn mình gợi ý thêm địa điểm, sắp xếp thứ tự tham quan hay phân tích ngân sách cho chuyến đi?",
  proposals: [],
}

const QUICK_PROMPTS = [
  "Tối ưu lại thứ tự ngày 1",
  "Gợi ý quán ăn trưa",
  "Dự báo thời tiết chuyến đi",
  "Kiểm tra ngân sách",
]

interface UndoWindow {
  proposalId: number
  until: number
}

function useSecondsLeft(until: number | null) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    if (until == null) return
    setNow(Date.now())
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [until])
  return until == null ? 0 : Math.max(0, Math.ceil((until - now) / 1000))
}

export function AiChatDrawer({ tripId, isOpen, onClose, onProposalApplied }: AiChatDrawerProps) {
  const [messages, setMessages] = useState<ChatMessageItem[]>([WELCOME])
  const [input, setInput] = useState("")
  const [isStreaming, setIsStreaming] = useState(false)
  const [currentTool, setCurrentTool] = useState<string | null>(null)
  const [conversationId, setConversationId] = useState<number | undefined>()
  const [busyProposalId, setBusyProposalId] = useState<number | null>(null)
  const [undo, setUndo] = useState<UndoWindow | null>(null)
  const abortRef = useRef<(() => void) | null>(null)
  const streamingIdRef = useRef<string | null>(null)
  const historySeededRef = useRef(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const secondsLeft = useSecondsLeft(undo?.until ?? null)
  const undoActive = undo != null && secondsLeft > 0

  const stopStreaming = useCallback(() => {
    abortRef.current?.()
    abortRef.current = null
    const streamingId = streamingIdRef.current
    if (streamingId) setMessages((prev) => finishStreaming(prev, streamingId))
    streamingIdRef.current = null
    setIsStreaming(false)
    setCurrentTool(null)
  }, [])

  // Abort the running turn when the drawer closes or unmounts
  useEffect(() => {
    if (!isOpen) stopStreaming()
  }, [isOpen, stopStreaming])
  useEffect(() => () => abortRef.current?.(), [])

  // A different trip starts a fresh conversation
  // biome-ignore lint/correctness/useExhaustiveDependencies: tripId is the reset trigger
  useEffect(() => {
    stopStreaming()
    setMessages([WELCOME])
    setConversationId(undefined)
    setUndo(null)
    historySeededRef.current = false
  }, [tripId, stopStreaming])

  const conversationsQuery = useQuery({
    queryKey: ["trip", tripId, "conversations"],
    queryFn: () => aiApi.getConversations(tripId),
    enabled: isOpen,
    staleTime: Number.POSITIVE_INFINITY,
  })
  const latestConversationId = conversationsQuery.data?.[0]?.id
  const historyQuery = useQuery({
    queryKey: ["conversation", latestConversationId, "messages"],
    queryFn: () => aiApi.getMessages(latestConversationId as number),
    enabled: isOpen && latestConversationId != null,
    staleTime: Number.POSITIVE_INFINITY,
  })

  // Resume the latest conversation once, unless the user already started chatting
  useEffect(() => {
    if (historySeededRef.current || !historyQuery.data || latestConversationId == null) return
    historySeededRef.current = true
    setMessages((prev) => {
      if (prev.length > 1) return prev
      return [WELCOME, ...messagesFromHistory(historyQuery.data)]
    })
    setConversationId((prev) => prev ?? latestConversationId)
  }, [historyQuery.data, latestConversationId])

  // biome-ignore lint/correctness/useExhaustiveDependencies: scroll whenever content changes
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages, currentTool])

  useEffect(() => {
    if (undo && secondsLeft === 0) setUndo(null)
  }, [undo, secondsLeft])

  const handleSend = () => {
    const userText = input.trim()
    if (!userText || isStreaming) return
    setInput("")

    const assistantId = newMessageId()
    streamingIdRef.current = assistantId
    historySeededRef.current = true
    setMessages((prev) => [
      ...prev,
      { id: newMessageId(), role: "user", text: userText, proposals: [] },
      { id: assistantId, role: "assistant", text: "", proposals: [], streaming: true },
    ])
    setIsStreaming(true)
    setCurrentTool(null)

    const done = () => {
      abortRef.current = null
      streamingIdRef.current = null
      setIsStreaming(false)
      setCurrentTool(null)
    }

    abortRef.current = aiApi.streamChat(
      tripId,
      { message: userText, conversationId },
      {
        onToken: (text) => setMessages((prev) => appendToken(prev, assistantId, text)),
        onToolStart: (evt) => setCurrentTool(evt.label || evt.tool),
        onToolEnd: () => setCurrentTool(null),
        onProposal: (evt) => setMessages((prev) => addProposal(prev, assistantId, proposalFromEvent(evt))),
        onDone: (evt) => {
          if (evt?.conversationId) setConversationId(evt.conversationId)
          setMessages((prev) => finishStreaming(prev, assistantId, "(Trợ lý không trả lời được lượt này)"))
          done()
        },
        onError: (err) => {
          setMessages((prev) => finishStreaming(prev, assistantId))
          done()
          toast.error(getErrorMessage(err, "Lỗi giao tiếp với Trợ lý AI"))
        },
      },
    )
  }

  const handleApplyProposal = async (proposalId: number) => {
    if (busyProposalId != null) return
    setBusyProposalId(proposalId)
    try {
      const res = await aiApi.applyProposal(tripId, proposalId)
      setMessages((prev) => setProposalStatus(prev, proposalId, "APPLIED"))
      const until = res.undoAvailableUntil ? Date.parse(res.undoAvailableUntil) : Number.NaN
      setUndo(Number.isFinite(until) && until > Date.now() ? { proposalId, until } : null)
      if (res.skipped?.length) {
        toast.warning(`Đã áp dụng ${res.applied} thay đổi, bỏ qua ${res.skipped.length}`, {
          description: res.skipped.map((sk) => `${sk.title || sk.op}: ${skipReasonLabel(sk.reason)}`).join("\n"),
        })
      } else {
        toast.success("Đã áp dụng đề xuất vào lịch trình thành công!")
      }
      onProposalApplied?.()
    } catch (err) {
      toast.error(getErrorMessage(err, "Không thể áp dụng đề xuất"))
    } finally {
      setBusyProposalId(null)
    }
  }

  const handleRejectProposal = async (proposalId: number) => {
    if (busyProposalId != null) return
    setBusyProposalId(proposalId)
    try {
      await aiApi.rejectProposal(proposalId)
      setMessages((prev) => setProposalStatus(prev, proposalId, "REJECTED"))
      toast.info("Đã từ chối đề xuất.")
    } catch (err) {
      toast.error(getErrorMessage(err, "Không thể từ chối đề xuất"))
    } finally {
      setBusyProposalId(null)
    }
  }

  const handleUndo = async () => {
    if (!undo) return
    const { proposalId } = undo
    try {
      const res = await aiApi.undoProposal(tripId, proposalId)
      setUndo(null)
      setMessages((prev) => setProposalStatus(prev, proposalId, "REVERTED"))
      if (res.undoSkipped?.length) {
        toast.warning("Đã hoàn tác, một số thay đổi được giữ lại", {
          description: res.undoSkipped.map((sk) => `${sk.title}: ${skipReasonLabel(sk.reason)}`).join("\n"),
        })
      } else {
        toast.success("Đã hoàn tác các thay đổi do AI đề xuất!")
      }
      onProposalApplied?.()
    } catch (err) {
      toast.error(getErrorMessage(err, "Lỗi khi hoàn tác"))
    }
  }

  const minutes = Math.floor(secondsLeft / 60)
  const seconds = String(secondsLeft % 60).padStart(2, "0")

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent>
        <div className="p-4 border-b border-border flex items-center justify-between bg-card/60">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 text-white shadow-md shadow-blue-500/20">
              <Sparkles className="h-5 w-5" aria-hidden />
            </div>
            <div>
              <DialogTitle className="font-bold text-sm leading-tight flex items-center gap-1.5">
                Trợ lý AI TripMind
                <Badge
                  variant="outline"
                  className="text-[10px] px-1.5 py-0 h-4 border-emerald-500/30 text-emerald-500 bg-emerald-500/10"
                >
                  Gemini ReAct
                </Badge>
              </DialogTitle>
              <DialogDescription>Phân tích dữ liệu & Đề xuất lịch trình</DialogDescription>
            </div>
          </div>
          <DialogClose asChild>
            <Button variant="ghost" size="icon" className="rounded-full" aria-label="Đóng trợ lý AI">
              <X className="h-4 w-4" />
            </Button>
          </DialogClose>
        </div>

        {undoActive && (
          <div className="bg-amber-500/10 border-b border-amber-500/20 px-4 py-2 flex items-center justify-between text-xs text-amber-600 dark:text-amber-400">
            <div className="flex items-center gap-1.5" aria-live="polite">
              <Clock className="h-3.5 w-3.5" aria-hidden />
              <span>
                Có thể hoàn tác đề xuất trong {minutes}:{seconds}
              </span>
            </div>
            <Button
              size="sm"
              variant="outline"
              onClick={handleUndo}
              className="h-7 text-xs gap-1 border-amber-500/30 text-amber-600 hover:bg-amber-500/10"
            >
              <RotateCcw className="h-3 w-3" /> Hoàn tác
            </Button>
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-4 space-y-4" aria-live="polite">
          {messages.map((m) => (
            <div key={m.id} className={`flex gap-3 ${m.role === "user" ? "justify-end" : "justify-start"}`}>
              {m.role === "assistant" && (
                <div className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  <Bot className="h-4 w-4" aria-hidden />
                </div>
              )}
              <div className={`max-w-[85%] space-y-2 ${m.role === "user" ? "items-end" : "items-start"}`}>
                {(m.text || m.streaming) && (
                  <div
                    className={`p-3 rounded-2xl text-sm leading-relaxed whitespace-pre-line ${
                      m.role === "user"
                        ? "bg-primary text-primary-foreground rounded-br-xs"
                        : "bg-muted/70 text-foreground rounded-bl-xs border border-border/50"
                    }`}
                  >
                    {m.text || <span className="text-muted-foreground italic">Đang suy nghĩ...</span>}
                  </div>
                )}
                {m.proposals.map((p) => (
                  <ProposalCard
                    key={p.proposalId}
                    proposal={p}
                    busy={busyProposalId != null}
                    onApply={handleApplyProposal}
                    onReject={handleRejectProposal}
                  />
                ))}
              </div>
              {m.role === "user" && (
                <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center shrink-0">
                  <UserIcon className="h-4 w-4" aria-hidden />
                </div>
              )}
            </div>
          ))}

          {currentTool && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted/40 p-2 rounded-lg border border-border/60 animate-pulse">
              <Wrench className="h-3.5 w-3.5 text-primary" aria-hidden />
              <span>
                AI đang chạy công cụ: <strong>{currentTool}</strong>...
              </span>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <div className="px-3 py-2 border-t border-border flex gap-1.5 overflow-x-auto text-[11px] no-scrollbar">
          {QUICK_PROMPTS.map((q) => (
            <button
              type="button"
              key={q}
              onClick={() => setInput(q)}
              className="px-2.5 py-1 rounded-full bg-muted hover:bg-accent text-muted-foreground hover:text-foreground whitespace-nowrap transition-colors cursor-pointer"
            >
              {q}
            </button>
          ))}
        </div>

        <div className="p-3 border-t border-border bg-card">
          <form
            onSubmit={(e) => {
              e.preventDefault()
              handleSend()
            }}
            className="flex gap-2"
          >
            <Input
              aria-label="Tin nhắn cho trợ lý AI"
              placeholder="Hỏi AI bất kỳ điều gì về chuyến đi..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={isStreaming}
              className="text-sm"
            />
            <Button type="submit" size="icon" disabled={isStreaming || !input.trim()} aria-label="Gửi tin nhắn">
              <Send className="h-4 w-4" />
            </Button>
          </form>
        </div>
      </SheetContent>
    </Dialog>
  )
}
