import { type CreateTripInput, tripApi } from "@/client/api/trip-api"
import { getErrorMessage } from "@/client/errors"
import type { DietaryRestriction, TravelStyle, Trip } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { queryKeys } from "@/hooks/query-keys"
import { type ParsedTripPlan, parseTripPromptWithAi } from "@/lib/prompt-parser"
import { PROMPT_MAX_LENGTH } from "@/lib/validation"
import { useQuery } from "@tanstack/react-query"
import {
  AlertTriangle,
  Calendar,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Compass,
  DollarSign,
  Heart,
  MapPin,
  Navigation,
  Sparkles,
  Users,
  Wand2,
} from "lucide-react"
import { useEffect, useState } from "react"
import { toast } from "sonner"
import {
  ChoiceGroup,
  Field,
  INTERESTS,
  LOCAL,
  LODGING,
  type LocalTransport,
  type LodgingType,
  PACES,
  QUICK_PROMPTS,
  STEPS,
  TRANSPORT,
  type TransportMode,
} from "./trip-form-parts"

interface CreateTripModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (newTrip: Trip) => void
  initialPrompt?: string
}

const orNull = (v: string) => (v.trim() ? v.trim() : null)
const isoDate = (offsetDays: number) => new Date(Date.now() + offsetDays * 86400000).toISOString().split("T")[0]

export function CreateTripModal({ isOpen, onClose, onSuccess, initialPrompt = "" }: CreateTripModalProps) {
  const [step, setStep] = useState(0)

  const [promptInput, setPromptInput] = useState(initialPrompt)
  const [isParsing, setIsParsing] = useState(false)
  const [parsedSummary, setParsedSummary] = useState<string | null>(null)
  const [aiFilled, setAiFilled] = useState<Set<string>>(new Set())

  // ① Hành trình
  const [title, setTitle] = useState("")
  const [origin, setOrigin] = useState("")
  const [destinationId, setDestinationId] = useState<number | null>(null)
  const [isCustomDestination, setIsCustomDestination] = useState(false)
  const [customDestName, setCustomDestName] = useState("")
  const [customCountry, setCustomCountry] = useState("Việt Nam")
  const [customLat, setCustomLat] = useState<number | null>(null)
  const [customLng, setCustomLng] = useState<number | null>(null)
  const [startDate, setStartDate] = useState(() => isoDate(1))
  const [endDate, setEndDate] = useState(() => isoDate(2))
  const [arrivalTime, setArrivalTime] = useState("")
  const [departureTime, setDepartureTime] = useState("")
  const [transportMode, setTransportMode] = useState<TransportMode | null>("AUTO")
  const [localTransport, setLocalTransport] = useState<LocalTransport | null>(null)

  // ② Nhóm đi
  const [travelers, setTravelers] = useState(2)
  const [childrenCount, setChildrenCount] = useState(0)
  const [childAges, setChildAges] = useState("")
  const [seniorsCount, setSeniorsCount] = useState(0)
  const [hasVegetarian, setHasVegetarian] = useState(false)
  const [hasHalal, setHasHalal] = useState(false)
  const [hasMobilityDifficulty, setHasMobilityDifficulty] = useState(false)
  const [motionSickness, setMotionSickness] = useState(false)
  const [allergies, setAllergies] = useState("")

  // ③ Sở thích & ngân sách
  const [interests, setInterests] = useState<string[]>([])
  const [travelStyle, setTravelStyle] = useState<TravelStyle>("BALANCED")
  const [lodgingType, setLodgingType] = useState<LodgingType | null>(null)
  const [lodgingName, setLodgingName] = useState("")
  const [budget, setBudget] = useState("5000000")
  const [budgetIncludesTransport, setBudgetIncludesTransport] = useState(true)
  const [avoid, setAvoid] = useState("")

  const [loading, setLoading] = useState(false)

  const { data: destinations = [] } = useQuery({
    queryKey: queryKeys.destinations,
    queryFn: () => tripApi.getDestinations(),
    enabled: isOpen,
  })

  const selectedDestinationId = destinationId ?? destinations[0]?.id ?? null
  const customMode = isCustomDestination || destinations.length === 0

  useEffect(() => {
    if (isOpen) setStep(0)
  }, [isOpen])

  const applyParsedPrompt = async (text: string) => {
    if (!text.trim()) return
    setIsParsing(true)
    try {
      const parsed: ParsedTripPlan = await parseTripPromptWithAi(text, destinations)
      const filled = new Set<string>(["title", "destination", "dates", "travelers", "budget", "pace"])

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
      if (parsed.origin) {
        setOrigin(parsed.origin)
        filled.add("origin")
      }
      if (parsed.transportMode) {
        setTransportMode(parsed.transportMode as TransportMode)
        filled.add("transport")
      }
      if (parsed.localTransport) {
        setLocalTransport(parsed.localTransport as LocalTransport)
        filled.add("local")
      }
      if (parsed.lodgingType) {
        setLodgingType(parsed.lodgingType as LodgingType)
        filled.add("lodging")
      }
      if (parsed.interests?.length) {
        setInterests(parsed.interests)
        filled.add("interests")
      }
      if (parsed.childAges?.length) {
        setChildAges(parsed.childAges.join(", "))
        filled.add("childAges")
      }
      if (parsed.motionSickness) setMotionSickness(true)
      if (parsed.avoid) {
        setAvoid(parsed.avoid)
        filled.add("avoid")
      }

      if (parsed.isCustomDestination || !parsed.destinationId) {
        setIsCustomDestination(true)
        setCustomDestName(parsed.customDestinationName || parsed.destinationName)
        setCustomCountry(parsed.customCountry || "Việt Nam")
        setCustomLat(parsed.customLat ?? null)
        setCustomLng(parsed.customLng ?? null)
      } else {
        setIsCustomDestination(false)
        setDestinationId(parsed.destinationId)
      }

      setAiFilled(filled)
      setParsedSummary(parsed.summary)
      toast.success("Đã điền sẵn theo mô tả, hãy kiểm tra các ô được đánh dấu ✨")
    } catch (err) {
      toast.error(getErrorMessage(err, "Không thể phân tích mô tả"))
    } finally {
      setIsParsing(false)
    }
  }

  // Auto-parse the prompt typed on the dashboard when the dialog opens
  // biome-ignore lint/correctness/useExhaustiveDependencies: run only when opened with a new prompt
  useEffect(() => {
    if (isOpen && initialPrompt) {
      setPromptInput(initialPrompt)
      applyParsedPrompt(initialPrompt)
    }
  }, [isOpen, initialPrompt])

  const ai = (key: string) => (aiFilled.has(key) ? "ring-2 ring-amber-400/70 bg-amber-500/5" : "")
  const destinationLabel = customMode
    ? customDestName || "(chưa nhập)"
    : destinations.find((d) => d.id === selectedDestinationId)?.name || "(chưa chọn)"
  const parsedAges = childAges
    .split(/[,\s]+/)
    .map((a) => Number.parseInt(a, 10))
    .filter((a) => !Number.isNaN(a) && a >= 0 && a <= 17)

  const step1Valid = !!((!customMode || customDestName.trim()) && startDate && endDate && endDate >= startDate)
  const warnings = [
    !origin.trim() && "Chưa có nơi xuất phát → lịch trình sẽ không có chặng đi/về và vé xe.",
    transportMode === "AUTO" && origin.trim() && "Phương tiện để AI chọn → AI sẽ đề xuất và ước tính giá vé.",
    !arrivalTime && "Chưa có giờ đến → AI tự ước tính theo quãng đường.",
    interests.length === 0 && "Chưa chọn sở thích → AI dùng các điểm nổi tiếng chung.",
    childrenCount > 0 && parsedAges.length === 0 && "Chưa nhập tuổi trẻ em → khó chọn hoạt động phù hợp.",
  ].filter(Boolean) as string[]

  const handleSubmit = async () => {
    setLoading(true)
    try {
      const dietary: DietaryRestriction[] = []
      if (hasVegetarian) dietary.push("VEGETARIAN")
      if (hasHalal) dietary.push("HALAL")
      const payload: CreateTripInput = {
        name: title.trim() || `${origin.trim() ? `${origin.trim()} → ` : ""}${destinationLabel} ${startDate}`,
        startDate,
        endDate,
        travelers,
        budget: budget ? Number(budget) : null,
        currency: "VND",
        travelStyle,
        preferences: interests,
        groupProfile: {
          children: childrenCount,
          seniors: seniorsCount,
          dietary,
          limitedMobility: hasMobilityDifficulty,
          childAges: parsedAges,
          motionSickness,
          allergies: orNull(allergies),
        },
        travelPlan: {
          origin: orNull(origin),
          arrivalTime: orNull(arrivalTime),
          departureTime: orNull(departureTime),
          transportMode,
          localTransport,
          lodgingType,
          lodgingName: orNull(lodgingName),
          budgetIncludesTransport,
          avoid: orNull(avoid),
        },
      }

      if (customMode || selectedDestinationId == null) {
        payload.destinationData = {
          provider: "MANUAL",
          name: customDestName.trim(),
          country: customCountry.trim() || "Việt Nam",
          // Left empty, the backend geocodes by name
          latitude: customLat,
          longitude: customLng,
          timezone: "Asia/Ho_Chi_Minh",
        }
      } else {
        payload.destinationId = selectedDestinationId
      }

      const created = await tripApi.createTrip(payload)
      toast.success("Tạo chuyến đi thành công!")
      onSuccess(created)
      onClose()
    } catch (err) {
      toast.error(getErrorMessage(err, "Không thể tạo chuyến đi"))
    } finally {
      setLoading(false)
    }
  }

  const groupChecks: Array<[boolean, (v: boolean) => void, string]> = [
    [hasVegetarian, setHasVegetarian, "Có người ăn chay"],
    [hasHalal, setHasHalal, "Ăn Halal"],
    [hasMobilityDifficulty, setHasMobilityDifficulty, "Đi lại khó khăn (hạn chế đi bộ)"],
    [motionSickness, setMotionSickness, "Có người say xe (tránh đèo dài)"],
  ]

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <div className="p-2.5 rounded-xl bg-primary/10 text-primary">
            <Compass className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle className="text-xl">Lập Kế Hoạch Chuyến Đi Mới</DialogTitle>
            <DialogDescription>Càng đủ thông tin, lịch trình AI càng sát thực tế</DialogDescription>
          </div>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto p-6 space-y-5">
          <div className="p-4 rounded-xl bg-gradient-to-br from-primary/15 via-primary/5 to-amber-500/10 border border-primary/25 space-y-3">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-primary" aria-hidden />
              <label htmlFor="trip-prompt" className="text-xs font-bold">
                Mô tả nhanh (không bắt buộc), AI sẽ điền sẵn các bước bên dưới
              </label>
            </div>
            <div className="flex gap-2">
              <Input
                id="trip-prompt"
                maxLength={PROMPT_MAX_LENGTH}
                placeholder="VD: tôi ở Hà Tĩnh, đi Tam Đảo 2N1Đ với vợ và bé 5 tuổi, đi xe khách, thích thiên nhiên..."
                value={promptInput}
                onChange={(e) => setPromptInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault()
                    applyParsedPrompt(promptInput)
                  }
                }}
                className="text-xs bg-background h-9 border-primary/30"
              />
              <Button
                type="button"
                size="sm"
                onClick={() => applyParsedPrompt(promptInput)}
                disabled={isParsing || !promptInput.trim()}
                className="h-9 px-3 text-xs gap-1.5 shrink-0 font-semibold cursor-pointer"
              >
                <Wand2 className="h-3.5 w-3.5" />
                {isParsing ? "Đang phân tích..." : "Phân tích & Điền"}
              </Button>
            </div>
            <div className="flex items-center gap-1.5 flex-wrap">
              {QUICK_PROMPTS.map((q) => (
                <button
                  type="button"
                  key={q.label}
                  onClick={() => {
                    setPromptInput(q.prompt)
                    applyParsedPrompt(q.prompt)
                  }}
                  className="px-2 py-0.5 rounded-full text-[10px] bg-background border border-primary/20 hover:border-primary cursor-pointer"
                >
                  {q.label}
                </button>
              ))}
            </div>
            {parsedSummary && (
              <div className="p-2.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5">
                <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden />
                <span>{parsedSummary}</span>
              </div>
            )}
          </div>

          <nav className="grid grid-cols-4 gap-1.5" aria-label="Các bước tạo chuyến đi">
            {STEPS.map((s, i) => (
              <button
                type="button"
                key={s.label}
                aria-current={step === i ? "step" : undefined}
                onClick={() => (i === 0 || step1Valid) && setStep(i)}
                className={`flex items-center gap-1.5 px-2 py-2 rounded-lg text-[11px] font-medium border cursor-pointer transition-all ${
                  step === i
                    ? "border-primary bg-primary/10 text-primary"
                    : i < step
                      ? "border-emerald-500/40 text-emerald-600 dark:text-emerald-400"
                      : "border-border text-muted-foreground"
                }`}
              >
                <s.icon className="h-3.5 w-3.5 shrink-0" aria-hidden />
                <span className="truncate">
                  {i + 1}. {s.label}
                </span>
              </button>
            ))}
          </nav>

          {step === 0 && (
            <div className="space-y-4 animate-in fade-in">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Field
                  label="Xuất phát từ"
                  icon={Navigation}
                  hint="Để AI xếp chặng đi/về và tính vé xe"
                  htmlFor="trip-origin"
                >
                  <Input
                    id="trip-origin"
                    placeholder="VD: Hà Tĩnh"
                    value={origin}
                    onChange={(e) => setOrigin(e.target.value)}
                    className={ai("origin")}
                  />
                </Field>
                <Field label="Điểm đến *" icon={MapPin} htmlFor="trip-destination">
                  <select
                    id="trip-destination"
                    className={`w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none cursor-pointer ${ai("destination")}`}
                    value={customMode ? -1 : (selectedDestinationId ?? -1)}
                    onChange={(e) => {
                      const val = Number(e.target.value)
                      setIsCustomDestination(val === -1)
                      if (val !== -1) setDestinationId(val)
                    }}
                  >
                    {destinations.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name} ({d.country || "Việt Nam"})
                      </option>
                    ))}
                    <option value={-1}>📍 Điểm đến khác (tự nhập)...</option>
                  </select>
                  {customMode && (
                    <div className="grid grid-cols-2 gap-2 pt-1">
                      <Input
                        aria-label="Tên điểm đến"
                        placeholder="Tên điểm đến"
                        value={customDestName}
                        onChange={(e) => {
                          setCustomDestName(e.target.value)
                          setCustomLat(null)
                          setCustomLng(null)
                        }}
                        className={`text-xs ${ai("destination")}`}
                      />
                      <Input
                        aria-label="Quốc gia"
                        placeholder="Quốc gia"
                        value={customCountry}
                        onChange={(e) => setCustomCountry(e.target.value)}
                        className="text-xs"
                      />
                    </div>
                  )}
                </Field>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Ngày đi *" icon={Calendar} htmlFor="trip-start">
                  <Input
                    id="trip-start"
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className={ai("dates")}
                  />
                </Field>
                <Field label="Ngày về *" icon={Calendar} htmlFor="trip-end">
                  <Input
                    id="trip-end"
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    className={ai("dates")}
                  />
                </Field>
                <Field label="Giờ có mặt ngày đầu" hint="Bỏ trống thì AI ước tính" htmlFor="trip-arrival">
                  <Input
                    id="trip-arrival"
                    type="time"
                    value={arrivalTime}
                    onChange={(e) => setArrivalTime(e.target.value)}
                  />
                </Field>
                <Field label="Giờ rời đi ngày cuối" hint="Bỏ trống thì AI ước tính" htmlFor="trip-departure">
                  <Input
                    id="trip-departure"
                    type="time"
                    value={departureTime}
                    onChange={(e) => setDepartureTime(e.target.value)}
                  />
                </Field>
              </div>

              <Field label="Phương tiện đến/về">
                <div className={`rounded-lg ${ai("transport")}`}>
                  <ChoiceGroup
                    label="Phương tiện đến/về"
                    options={TRANSPORT}
                    value={transportMode}
                    onChange={setTransportMode}
                  />
                </div>
              </Field>
              <Field label="Đi lại tại điểm đến">
                <div className={`rounded-lg ${ai("local")}`}>
                  <ChoiceGroup
                    label="Đi lại tại điểm đến"
                    options={LOCAL}
                    value={localTransport}
                    onChange={setLocalTransport}
                  />
                </div>
              </Field>
            </div>
          )}

          {step === 1 && (
            <div className="space-y-4 animate-in fade-in">
              <div className="grid grid-cols-3 gap-3">
                <Field label="Tổng số người *" icon={Users} htmlFor="trip-travelers">
                  <Input
                    id="trip-travelers"
                    type="number"
                    min="1"
                    max="50"
                    value={travelers}
                    onChange={(e) => setTravelers(Number(e.target.value))}
                    className={ai("travelers")}
                  />
                </Field>
                <Field label="Trẻ em (<12 tuổi)" htmlFor="trip-children">
                  <Input
                    id="trip-children"
                    type="number"
                    min="0"
                    value={childrenCount}
                    onChange={(e) => setChildrenCount(Number(e.target.value))}
                  />
                </Field>
                <Field label="Người cao tuổi" htmlFor="trip-seniors">
                  <Input
                    id="trip-seniors"
                    type="number"
                    min="0"
                    value={seniorsCount}
                    onChange={(e) => setSeniorsCount(Number(e.target.value))}
                  />
                </Field>
              </div>
              {childrenCount > 0 && (
                <Field label="Tuổi các bé" hint="Cách nhau dấu phẩy, VD: 5, 8" htmlFor="trip-child-ages">
                  <Input
                    id="trip-child-ages"
                    placeholder="5, 8"
                    value={childAges}
                    onChange={(e) => setChildAges(e.target.value)}
                    className={ai("childAges")}
                  />
                </Field>
              )}
              <div className="grid grid-cols-2 gap-2 text-xs">
                {groupChecks.map(([checked, set, label]) => (
                  <label
                    key={label}
                    className="flex items-center gap-2 p-2.5 rounded-lg border border-border cursor-pointer hover:bg-muted"
                  >
                    <input type="checkbox" checked={checked} onChange={(e) => set(e.target.checked)} />
                    <span>{label}</span>
                  </label>
                ))}
              </div>
              <Field label="Dị ứng / kiêng ăn" htmlFor="trip-allergies">
                <Input
                  id="trip-allergies"
                  placeholder="VD: hải sản, đậu phộng"
                  value={allergies}
                  onChange={(e) => setAllergies(e.target.value)}
                />
              </Field>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-4 animate-in fade-in">
              <Field
                label="Muốn trải nghiệm"
                icon={Heart}
                hint="Dùng để tìm địa điểm trên Google Places theo đúng sở thích"
              >
                <div className={`flex flex-wrap gap-1.5 rounded-lg ${ai("interests")}`}>
                  {INTERESTS.map((it) => (
                    <button
                      type="button"
                      key={it}
                      aria-pressed={interests.includes(it)}
                      onClick={() =>
                        setInterests((cur) => (cur.includes(it) ? cur.filter((x) => x !== it) : [...cur, it]))
                      }
                      className={`px-3 py-1.5 rounded-full border text-xs cursor-pointer ${
                        interests.includes(it)
                          ? "border-primary bg-primary/10 text-primary font-semibold"
                          : "border-border text-muted-foreground hover:bg-muted"
                      }`}
                    >
                      {it}
                    </button>
                  ))}
                </div>
              </Field>
              <Field label="Nhịp độ">
                <div className="grid grid-cols-3 gap-2">
                  {PACES.map((p) => (
                    <button
                      type="button"
                      key={p.id}
                      aria-pressed={travelStyle === p.id}
                      onClick={() => setTravelStyle(p.id)}
                      className={`p-3 rounded-lg border text-left cursor-pointer ${
                        travelStyle === p.id
                          ? "border-primary bg-primary/10 text-primary ring-1 ring-primary"
                          : "border-border text-muted-foreground hover:bg-muted"
                      }`}
                    >
                      <div className="font-semibold text-xs">{p.label}</div>
                      <div className="text-[10px] opacity-80">{p.desc}</div>
                    </button>
                  ))}
                </div>
              </Field>
              <Field label="Nơi ở">
                <div className={`rounded-lg ${ai("lodging")}`}>
                  <ChoiceGroup label="Nơi ở" options={LODGING} value={lodgingType} onChange={setLodgingType} />
                </div>
                {lodgingType === "BOOKED" && (
                  <Input
                    aria-label="Tên nơi ở đã đặt"
                    placeholder="Tên khách sạn/homestay đã đặt"
                    value={lodgingName}
                    onChange={(e) => setLodgingName(e.target.value)}
                    className="mt-2"
                  />
                )}
              </Field>
              <Field label="Ngân sách (VND)" icon={DollarSign} htmlFor="trip-budget">
                <Input
                  id="trip-budget"
                  type="number"
                  min={0}
                  step="100000"
                  value={budget}
                  onChange={(e) => setBudget(e.target.value)}
                  className={ai("budget")}
                />
                <label className="flex items-center gap-2 text-xs pt-1 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={budgetIncludesTransport}
                    onChange={(e) => setBudgetIncludesTransport(e.target.checked)}
                  />
                  Đã gồm vé đi/về
                </label>
              </Field>
              <Field label="Không muốn" htmlFor="trip-avoid">
                <Input
                  id="trip-avoid"
                  placeholder="VD: chỗ đông người, leo bộ xa"
                  value={avoid}
                  onChange={(e) => setAvoid(e.target.value)}
                  className={ai("avoid")}
                />
              </Field>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-4 animate-in fade-in">
              <Field label="Tên chuyến đi" htmlFor="trip-name">
                <Input
                  id="trip-name"
                  maxLength={160}
                  placeholder="Bỏ trống để tự đặt tên"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className={ai("title")}
                />
              </Field>
              <div className="p-4 rounded-xl bg-muted/40 border border-border text-xs space-y-1.5">
                <div>
                  <b>Hành trình:</b> {origin.trim() || "?"} → {destinationLabel} · {startDate} → {endDate}
                  {arrivalTime && ` · đến ${arrivalTime}`}
                  {departureTime && ` · về ${departureTime}`}
                </div>
                <div>
                  <b>Phương tiện:</b> {TRANSPORT.find((t) => t.id === transportMode)?.label || "chưa chọn"}
                  {localTransport && `, tại chỗ: ${LOCAL.find((t) => t.id === localTransport)?.label}`}
                </div>
                <div>
                  <b>Nhóm:</b> {travelers} người
                  {childrenCount > 0 &&
                    `, ${childrenCount} trẻ em${parsedAges.length ? ` (${parsedAges.join(", ")} tuổi)` : ""}`}
                  {seniorsCount > 0 && `, ${seniorsCount} người cao tuổi`}
                  {hasVegetarian && ", ăn chay"}
                  {motionSickness && ", say xe"}
                </div>
                <div>
                  <b>Sở thích:</b> {interests.join(", ") || "chưa chọn"} ·{" "}
                  {PACES.find((p) => p.id === travelStyle)?.label}
                  {lodgingType && ` · ở ${LODGING.find((l) => l.id === lodgingType)?.label}`}
                </div>
                <div>
                  <b>Ngân sách:</b> {new Intl.NumberFormat("vi-VN").format(Number(budget) || 0)}đ (
                  {budgetIncludesTransport ? "gồm" : "không gồm"} vé đi/về)
                </div>
              </div>
              {warnings.length > 0 && (
                <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-xs space-y-1 text-amber-700 dark:text-amber-300">
                  {warnings.map((w) => (
                    <div key={w} className="flex gap-1.5">
                      <AlertTriangle className="h-3.5 w-3.5 shrink-0 mt-0.5" aria-hidden /> {w}
                    </div>
                  ))}
                </div>
              )}
              {aiFilled.size > 0 && (
                <Badge variant="outline" className="text-[10px] border-amber-400/60 text-amber-600">
                  ✨ Ô viền vàng là AI điền sẵn, hãy kiểm tra lại
                </Badge>
              )}
            </div>
          )}
        </div>

        <div className="p-4 flex justify-between gap-3 border-t">
          <Button
            type="button"
            variant="outline"
            onClick={() => (step === 0 ? onClose() : setStep(step - 1))}
            className="cursor-pointer gap-1"
          >
            {step === 0 ? (
              "Hủy"
            ) : (
              <>
                <ChevronLeft className="h-4 w-4" /> Quay lại
              </>
            )}
          </Button>
          {step < STEPS.length - 1 ? (
            <Button
              type="button"
              disabled={step === 0 && !step1Valid}
              onClick={() => setStep(step + 1)}
              className="cursor-pointer gap-1 font-semibold"
            >
              Tiếp <ChevronRight className="h-4 w-4" />
            </Button>
          ) : (
            <Button
              type="button"
              disabled={loading || !step1Valid}
              onClick={handleSubmit}
              className="cursor-pointer font-semibold"
            >
              {loading ? "Đang tạo..." : "Khởi tạo chuyến đi"}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
