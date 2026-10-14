import type { TravelPlan, TravelStyle } from "@/client/types"
import { ClipboardCheck, Heart, type LucideIcon, Navigation, Users } from "lucide-react"

export type Option<T extends string = string> = { id: T; label: string }

export const STEPS = [
  { label: "Hành trình", icon: Navigation },
  { label: "Nhóm đi", icon: Users },
  { label: "Sở thích & ngân sách", icon: Heart },
  { label: "Xem lại", icon: ClipboardCheck },
]

export type TransportMode = NonNullable<TravelPlan["transportMode"]>
export type LocalTransport = NonNullable<TravelPlan["localTransport"]>
export type LodgingType = NonNullable<TravelPlan["lodgingType"]>

export const TRANSPORT: Option<TransportMode>[] = [
  { id: "AUTO", label: "AI chọn" },
  { id: "BUS", label: "Xe khách" },
  { id: "TRAIN", label: "Tàu" },
  { id: "FLIGHT", label: "Máy bay" },
  { id: "CAR", label: "Ô tô riêng" },
  { id: "MOTORBIKE", label: "Xe máy" },
]

export const LOCAL: Option<LocalTransport>[] = [
  { id: "TAXI", label: "Taxi/Grab" },
  { id: "MOTORBIKE_RENTAL", label: "Thuê xe máy" },
  { id: "OWN_VEHICLE", label: "Xe riêng" },
  { id: "WALK", label: "Đi bộ" },
]

export const LODGING: Option<LodgingType>[] = [
  { id: "HOMESTAY", label: "Homestay" },
  { id: "HOTEL", label: "Khách sạn" },
  { id: "RESORT", label: "Resort" },
  { id: "BOOKED", label: "Đã đặt sẵn" },
]

export const INTERESTS = ["thiên nhiên", "check-in", "ẩm thực", "văn hóa tâm linh", "mạo hiểm", "nghỉ dưỡng", "mua sắm"]

export const PACES: Array<Option<TravelStyle> & { desc: string }> = [
  { id: "RELAXED", label: "Thư thả", desc: "2-3 điểm/ngày" },
  { id: "BALANCED", label: "Cân bằng", desc: "4 điểm/ngày" },
  { id: "FAST_PACED", label: "Dày đặc", desc: "5-6 điểm/ngày" },
]

export const QUICK_PROMPTS = [
  {
    label: "🏔️ Hà Tĩnh → Tam Đảo 2N1Đ",
    prompt: "tôi ở Hà Tĩnh, đi Tam Đảo 2 ngày 1 đêm với vợ và 1 bé 5 tuổi, đi xe khách",
  },
  {
    label: "🌸 Đà Lạt 3N2Đ",
    prompt: "từ Hà Nội đi đà lạt 3 ngày 2 đêm cùng người yêu bằng máy bay, ngân sách 8 triệu, thích check-in",
  },
  {
    label: "🏝️ Phú Quốc gia đình",
    prompt: "từ TP.HCM đi phú quốc 4 ngày 3 đêm cho gia đình có 2 bé nhỏ, ở resort, ngân sách 15 triệu",
  },
  {
    label: "🌊 Quy Nhơn ăn chay",
    prompt: "từ Đà Nẵng đi quy nhơn 3 ngày 2 đêm 2 người bằng tàu, ngân sách 6 triệu, ăn chay",
  },
]

export function ChoiceGroup<T extends string>({
  options,
  value,
  onChange,
  label,
}: {
  options: Option<T>[]
  value: T | null
  onChange: (v: T | null) => void
  label: string
}) {
  return (
    <fieldset className="flex flex-wrap gap-1.5">
      <legend className="sr-only">{label}</legend>
      {options.map((o) => (
        <button
          type="button"
          key={o.id}
          aria-pressed={value === o.id}
          onClick={() => onChange(value === o.id ? null : o.id)}
          className={`px-3 py-1.5 rounded-lg border text-xs cursor-pointer transition-all ${
            value === o.id
              ? "border-primary bg-primary/10 text-primary font-semibold ring-1 ring-primary"
              : "border-border hover:bg-muted text-muted-foreground"
          }`}
        >
          {o.label}
        </button>
      ))}
    </fieldset>
  )
}

/** Form row; pass htmlFor when the child is a single input. */
export function Field({
  label,
  icon: Icon,
  hint,
  htmlFor,
  children,
}: {
  label: string
  icon?: LucideIcon
  hint?: string
  htmlFor?: string
  children: React.ReactNode
}) {
  const content = (
    <>
      {Icon && <Icon className="h-4 w-4 text-primary" aria-hidden />} {label}
    </>
  )
  return (
    <div className="space-y-1.5">
      {htmlFor ? (
        <label htmlFor={htmlFor} className="text-sm font-medium flex items-center gap-1.5">
          {content}
        </label>
      ) : (
        <span className="text-sm font-medium flex items-center gap-1.5">{content}</span>
      )}
      {children}
      {hint && <p className="text-[11px] text-muted-foreground">{hint}</p>}
    </div>
  )
}
