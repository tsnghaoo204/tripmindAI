import React, { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  X,
  Calendar,
  MapPin,
  Users,
  DollarSign,
  Sparkles,
  Compass,
  Wand2,
  CheckCircle2,
  Send,
  Globe,
} from "lucide-react"
import { toast } from "sonner"
import { apiClient } from "@/client/client"
import { parseTripPromptWithAi, type ParsedTripPlan } from "@/lib/prompt-parser"

interface CreateTripModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (newTrip: any) => void
  initialPrompt?: string
}

export function CreateTripModal({
  isOpen,
  onClose,
  onSuccess,
  initialPrompt = "",
}: CreateTripModalProps) {
  // Destinations catalog loaded dynamically from backend
  const [destinations, setDestinations] = useState<Array<{ id: number; name: string; country?: string }>>([])

  // Chat Prompt Parser States
  const [promptInput, setPromptInput] = useState(initialPrompt)
  const [isParsing, setIsParsing] = useState(false)
  const [parsedSummary, setParsedSummary] = useState<string | null>(null)
  const [highlightFields, setHighlightFields] = useState(false)

  // Form Fields
  const [title, setTitle] = useState("")
  const [destinationId, setDestinationId] = useState<number | -1>(1)
  const [isCustomDestination, setIsCustomDestination] = useState(false)
  const [customDestName, setCustomDestName] = useState("")
  const [customCountry, setCustomCountry] = useState("Việt Nam")
  const [customLat, setCustomLat] = useState(16.0)
  const [customLng, setCustomLng] = useState(108.0)

  const [startDate, setStartDate] = useState(
    new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().split("T")[0],
  )
  const [endDate, setEndDate] = useState(
    new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
  )
  const [travelers, setTravelers] = useState(2)
  const [budget, setBudget] = useState("5000000")
  const [travelStyle, setTravelStyle] = useState<"RELAXED" | "BALANCED" | "FAST_PACED">("BALANCED")
  const [childrenCount, setChildrenCount] = useState(0)
  const [seniorsCount, setSeniorsCount] = useState(0)
  const [hasVegetarian, setHasVegetarian] = useState(false)
  const [hasMobilityDifficulty, setHasMobilityDifficulty] = useState(false)
  const [loading, setLoading] = useState(false)

  // Fetch full destinations dynamically from backend on open
  useEffect(() => {
    if (!isOpen) return
    const fetchDestinations = async () => {
      try {
        const res = await apiClient.get("/api/destinations")
        if (res.data?.success && Array.isArray(res.data?.data) && res.data.data.length > 0) {
          setDestinations(res.data.data)
          // Default to first destination or find matching
          if (!isCustomDestination) {
            setDestinationId(res.data.data[0].id)
          }
        }
      } catch {
        // Fallback: destinations will be populated when available
      }
    }
    fetchDestinations()
  }, [isOpen])

  // Handle initialPrompt on open
  useEffect(() => {
    if (isOpen && initialPrompt) {
      setPromptInput(initialPrompt)
      applyParsedPrompt(initialPrompt)
    }
  }, [isOpen, initialPrompt])

  const applyParsedPrompt = async (textToParse: string) => {
    if (!textToParse.trim()) return
    setIsParsing(true)

    try {
      const parsed: ParsedTripPlan = await parseTripPromptWithAi(textToParse, destinations)

      setTitle(parsed.title)
      setTravelers(parsed.travelers)
      setStartDate(parsed.startDate)
      setEndDate(parsed.endDate)
      setBudget(String(parsed.budget))
      setTravelStyle(parsed.travelStyle)
      setChildrenCount(parsed.childrenCount)
      setSeniorsCount(parsed.seniorsCount)
      setHasVegetarian(parsed.hasVegetarian)
      setHasMobilityDifficulty(parsed.hasMobilityDifficulty)

      if (parsed.isCustomDestination || !parsed.destinationId) {
        setIsCustomDestination(true)
        setDestinationId(-1)
        setCustomDestName(parsed.customDestinationName || parsed.destinationName)
        setCustomCountry(parsed.customCountry || "Việt Nam")
        if (parsed.customLat) setCustomLat(parsed.customLat)
        if (parsed.customLng) setCustomLng(parsed.customLng)
      } else {
        setIsCustomDestination(false)
        setDestinationId(parsed.destinationId)
      }

      setParsedSummary(parsed.summary)
      setHighlightFields(true)
      setTimeout(() => setHighlightFields(false), 2500)

      toast.success("Đã tự động điền các trường theo mô tả của bạn!")
    } catch {
      toast.error("Không thể phân tích mô tả")
    } finally {
      setIsParsing(false)
    }
  }

  const handleManualParse = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    applyParsedPrompt(promptInput)
  }

  const handleQuickChip = (chipPrompt: string) => {
    setPromptInput(chipPrompt)
    applyParsedPrompt(chipPrompt)
  }

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const payload: any = {
        name: title.trim() || `Kế hoạch du lịch ${startDate}`,
        title: title.trim() || `Kế hoạch du lịch ${startDate}`,
        startDate,
        endDate,
        travelers,
        budget: budget ? Number(budget) : null,
        currency: "VND",
        travelStyle,
        groupProfile: {
          childrenCount,
          seniorsCount,
          hasVegetarian,
          hasHalal: false,
          hasMobilityDifficulty,
        },
      }

      // Handle custom or standard destination
      if (isCustomDestination || destinationId === -1) {
        payload.destinationData = {
          provider: "MANUAL",
          name: customDestName.trim() || "Điểm đến tùy chỉnh",
          country: customCountry.trim() || "Việt Nam",
          latitude: customLat || 16.0544,
          longitude: customLng || 108.2022,
          timezone: "Asia/Ho_Chi_Minh",
        }
      } else {
        payload.destinationId = destinationId
      }

      const res = await apiClient.post("/api/trips", payload)
      if (res.data?.success) {
        toast.success("Tạo chuyến đi thành công!")
        onSuccess(res.data.data)
        onClose()
      } else {
        toast.error(res.data?.message || "Không thể tạo chuyến đi")
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Lỗi kết nối máy chủ")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
      <Card className="w-full max-w-xl max-h-[92vh] flex flex-col shadow-2xl border-border animate-in fade-in-50 zoom-in-95">
        <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
          <div className="flex items-center gap-2.5">
            <div className="p-2.5 rounded-xl bg-primary/10 text-primary">
              <Compass className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-xl font-bold">Lập Kế Hoạch Chuyến Đi Mới</CardTitle>
              <p className="text-xs text-muted-foreground mt-0.5">
                Thiết lập hồ sơ để AI cá nhân hóa lịch trình sát với nhóm của bạn
              </p>
            </div>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} className="rounded-full">
            <X className="h-4 w-4" />
          </Button>
        </CardHeader>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {/* KHUNG CHAT AI: Nhập mô tả để tự động parse vào form */}
          <div className="p-4 rounded-xl bg-gradient-to-br from-primary/15 via-primary/5 to-amber-500/10 border border-primary/25 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-primary animate-pulse" />
                <span className="text-xs font-bold text-foreground">
                  Khung Chat AI: Nhập mô tả để tự động điền form
                </span>
              </div>
              <Badge variant="outline" className="text-[10px] bg-primary/10 text-primary border-primary/30 font-semibold">
                AI Auto-Fill
              </Badge>
            </div>

            <div className="flex gap-2">
              <Input
                placeholder="VD: tôi muốn lên kế hoạch đi nha trang 2 ngày 1 đêm cho 2 người, ngân sách 4 triệu..."
                value={promptInput}
                onChange={(e) => setPromptInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault()
                    handleManualParse()
                  }
                }}
                className="text-xs bg-background h-9 border-primary/30 focus-visible:ring-primary shadow-inner"
              />
              <Button
                type="button"
                size="sm"
                onClick={() => handleManualParse()}
                disabled={isParsing || !promptInput.trim()}
                className="h-9 px-3 text-xs gap-1.5 shrink-0 bg-primary text-primary-foreground font-semibold shadow cursor-pointer hover:opacity-95"
              >
                <Wand2 className="h-3.5 w-3.5" />
                {isParsing ? "Đang parse..." : "Phân tích & Điền"}
              </Button>
            </div>

            {/* Quick chips suggestions */}
            <div className="flex items-center gap-1.5 flex-wrap text-xs pt-0.5">
              <span className="text-muted-foreground text-[10px] font-medium">Gợi ý mẫu:</span>
              <button
                type="button"
                onClick={() => handleQuickChip("tôi muốn lên kế hoạch đi nha trang 2 ngày 1 đêm")}
                className="px-2 py-0.5 rounded-full text-[10px] bg-background border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
              >
                🏖️ Nha Trang 2N1Đ
              </button>
              <button
                type="button"
                onClick={() => handleQuickChip("tôi muốn đi đà lạt 3 ngày 2 đêm cùng người yêu, ngân sách 5 triệu")}
                className="px-2 py-0.5 rounded-full text-[10px] bg-background border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
              >
                🌸 Đà Lạt 3N2Đ lãng mạn
              </button>
              <button
                type="button"
                onClick={() => handleQuickChip("kế hoạch đi phú quốc 4 ngày 3 đêm cho gia đình có 2 bé nhỏ, ngân sách 15 triệu")}
                className="px-2 py-0.5 rounded-full text-[10px] bg-background border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
              >
                🏝️ Phú Quốc 4N3Đ gia đình
              </button>
              <button
                type="button"
                onClick={() => handleQuickChip("đi quy nhơn 3 ngày 2 đêm 2 người, ngân sách 6 triệu, ăn chay")}
                className="px-2 py-0.5 rounded-full text-[10px] bg-background border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
              >
                🌊 Quy Nhơn 3N2Đ ăn chay
              </button>
              <button
                type="button"
                onClick={() => handleQuickChip("lên kế hoạch đi côn đảo 3 ngày 2 đêm cho 4 người")}
                className="px-2 py-0.5 rounded-full text-[10px] bg-background border border-primary/20 hover:border-primary text-foreground transition-all cursor-pointer hover:scale-105"
              >
                📍 Côn Đảo (Địa điểm khác)
              </button>
            </div>

            {/* Parsed Alert */}
            {parsedSummary && (
              <div className="p-2.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-600 dark:text-emerald-400 flex items-center justify-between animate-in fade-in zoom-in-95">
                <div className="flex items-center gap-1.5 font-medium">
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500" />
                  <span>{parsedSummary}</span>
                </div>
                <Badge variant="outline" className="text-[9px] border-emerald-500/40 text-emerald-600 bg-emerald-500/15">
                  Đã tự động điền form &darr;
                </Badge>
              </div>
            )}
          </div>

          {/* Form Fields: Tên chuyến đi */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">Tên chuyến đi</label>
            <Input
              required
              placeholder="VD: Kỳ nghỉ hè Đà Nẵng cùng gia đình"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className={highlightFields ? "ring-2 ring-primary transition-all duration-300" : ""}
            />
          </div>

          {/* Điểm đến & Số người */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-sm font-medium flex items-center justify-between">
                <span className="flex items-center gap-1.5">
                  <MapPin className="h-4 w-4 text-primary" /> Điểm đến
                </span>
                {isCustomDestination && (
                  <Badge variant="secondary" className="text-[9px] text-amber-600 dark:text-amber-400">
                    Tự nhập điểm đến
                  </Badge>
                )}
              </label>

              <select
                className={`w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:ring-2 focus:ring-ring outline-none cursor-pointer ${
                  highlightFields ? "ring-2 ring-primary transition-all duration-300" : ""
                }`}
                value={isCustomDestination ? -1 : destinationId}
                onChange={(e) => {
                  const val = Number(e.target.value)
                  if (val === -1) {
                    setIsCustomDestination(true)
                    setDestinationId(-1)
                  } else {
                    setIsCustomDestination(false)
                    setDestinationId(val)
                  }
                }}
              >
                {destinations.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name} ({d.country || "Việt Nam"})
                  </option>
                ))}
                <option value={-1} className="font-bold text-primary">
                  📍 Điểm đến khác (Tự nhập tên)...
                </option>
              </select>

              {/* Ô tự nhập khi chọn điểm đến khác */}
              {isCustomDestination && (
                <div className="pt-2 space-y-2 animate-in fade-in">
                  <Input
                    required
                    placeholder="Nhập tên địa điểm (VD: Côn Đảo, Mộc Châu, Paris...)"
                    value={customDestName}
                    onChange={(e) => setCustomDestName(e.target.value)}
                    className="text-xs"
                  />
                  <Input
                    placeholder="Quốc gia (Mặc định: Việt Nam)"
                    value={customCountry}
                    onChange={(e) => setCustomCountry(e.target.value)}
                    className="text-xs"
                  />
                </div>
              )}
            </div>

            <div className="space-y-1.5">
              <label className="text-sm font-medium flex items-center gap-1.5">
                <Users className="h-4 w-4 text-primary" /> Số người tham gia
              </label>
              <Input
                type="number"
                min="1"
                max="50"
                value={travelers}
                onChange={(e) => setTravelers(Number(e.target.value))}
                required
                className={highlightFields ? "ring-2 ring-primary transition-all duration-300" : ""}
              />
            </div>
          </div>

          {/* Ngày đi & Ngày về */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-sm font-medium flex items-center gap-1.5">
                <Calendar className="h-4 w-4 text-primary" /> Ngày đi
              </label>
              <Input
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className={highlightFields ? "ring-2 ring-primary transition-all duration-300" : ""}
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-sm font-medium flex items-center gap-1.5">
                <Calendar className="h-4 w-4 text-primary" /> Ngày về (Tối đa 30 ngày)
              </label>
              <Input
                type="date"
                required
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className={highlightFields ? "ring-2 ring-primary transition-all duration-300" : ""}
              />
            </div>
          </div>

          {/* Dự toán ngân sách */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium flex items-center gap-1.5">
              <DollarSign className="h-4 w-4 text-emerald-500" /> Dự toán ngân sách (VND)
            </label>
            <Input
              type="number"
              step="100000"
              placeholder="VD: 5000000"
              value={budget}
              onChange={(e) => setBudget(e.target.value)}
              className={highlightFields ? "ring-2 ring-primary transition-all duration-300" : ""}
            />
          </div>

          {/* Nhịp độ du lịch */}
          <div className="space-y-2">
            <label className="text-sm font-medium">Nhịp độ du lịch</label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { id: "RELAXED", label: "Thư thả", desc: "2-3 điểm/ngày" },
                { id: "BALANCED", label: "Cân bằng", desc: "4 điểm/ngày" },
                { id: "FAST_PACED", label: "Trải nghiệm", desc: "5-6 điểm/ngày" },
              ].map((style) => (
                <button
                  type="button"
                  key={style.id}
                  onClick={() => setTravelStyle(style.id as any)}
                  className={`p-3 rounded-lg border text-left cursor-pointer transition-all ${
                    travelStyle === style.id
                      ? "border-primary bg-primary/10 text-primary font-medium shadow-sm ring-1 ring-primary"
                      : "border-border hover:bg-muted text-muted-foreground"
                  }`}
                >
                  <div className="font-semibold text-xs">{style.label}</div>
                  <div className="text-[10px] opacity-80">{style.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Hồ sơ nhóm đi (AI Tối Ưu) */}
          <div className="p-4 rounded-xl bg-muted/40 border border-border/80 space-y-3">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-amber-500" />
              <span className="text-xs font-semibold uppercase tracking-wider">Thông tin nhóm đi (AI tối ưu)</span>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div>
                <label className="block mb-1 text-muted-foreground">Trẻ nhỏ (dưới 12 tuổi)</label>
                <Input
                  type="number"
                  min="0"
                  value={childrenCount}
                  onChange={(e) => setChildrenCount(Number(e.target.value))}
                  className={highlightFields && childrenCount > 0 ? "ring-2 ring-primary" : ""}
                />
              </div>
              <div>
                <label className="block mb-1 text-muted-foreground">Người cao tuổi (&gt; 60 tuổi)</label>
                <Input
                  type="number"
                  min="0"
                  value={seniorsCount}
                  onChange={(e) => setSeniorsCount(Number(e.target.value))}
                  className={highlightFields && seniorsCount > 0 ? "ring-2 ring-primary" : ""}
                />
              </div>
            </div>

            <div className="flex flex-wrap gap-4 pt-1 text-xs">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={hasVegetarian}
                  onChange={(e) => setHasVegetarian(e.target.checked)}
                  className="rounded border-border text-primary focus:ring-primary"
                />
                <span>Có người ăn chay (AI tự ưu tiên quán chay)</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={hasMobilityDifficulty}
                  onChange={(e) => setHasMobilityDifficulty(e.target.checked)}
                  className="rounded border-border text-primary focus:ring-primary"
                />
                <span>Đi lại khó khăn (Hạn chế đi bộ xa)</span>
              </label>
            </div>
          </div>

          {/* Action buttons */}
          <div className="pt-2 flex justify-end gap-3 border-t">
            <Button type="button" variant="outline" onClick={onClose} className="cursor-pointer">
              Hủy
            </Button>
            <Button type="submit" disabled={loading} className="gap-2 cursor-pointer bg-primary text-primary-foreground font-semibold shadow">
              {loading ? "Đang tạo..." : "Khởi tạo chuyến đi"}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
