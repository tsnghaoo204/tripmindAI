import { apiClient } from "@/client/client"

export interface ParsedTripPlan {
  title: string
  destinationId: number | null
  destinationName: string
  isCustomDestination: boolean
  customDestinationName: string
  customCountry: string
  customLat?: number
  customLng?: number
  customTimezone?: string
  startDate: string
  endDate: string
  days: number
  nights: number
  travelers: number
  budget: number
  travelStyle: "RELAXED" | "BALANCED" | "FAST_PACED"
  childrenCount: number
  seniorsCount: number
  hasVegetarian: boolean
  hasMobilityDifficulty: boolean
  summary: string
}

/**
 * Phân tích mô tả chuyến đi bằng AI thông qua backend API `/api/trips/parse-prompt`.
 * KHÔNG fix cứng bất kỳ danh sách thành phố nào ở frontend.
 */
export async function parseTripPromptWithAi(
  prompt: string,
  availableDestinations: Array<{ id: number; name: string }> = [],
): Promise<ParsedTripPlan> {
  const p = prompt.trim()

  // 1. Gọi backend AI Parser (Gemini 3.8 Flash xử lý ngôn ngữ tự nhiên linh hoạt)
  try {
    const res = await apiClient.post("/api/trips/parse-prompt", { prompt: p })
    if (res.data?.success && res.data?.data && res.data.data.destinationName) {
      const data = res.data.data
      const destName = data.destinationName || "Điểm đến"
      const days = data.days || 3
      const nights = data.nights || Math.max(1, days - 1)
      const travelers = data.travelers || 2
      const budget = data.budget || (days * travelers * 800000)

      // Kiểm tra xem điểm đến trả về có sẵn trong danh mục CSDL không
      let matchedId = data.destinationId || null
      let isCustom = data.isCustomDestination ?? false

      if (!matchedId && availableDestinations.length > 0) {
        const found = availableDestinations.find(
          (d) => d.name.toLowerCase() === destName.toLowerCase() ||
                 destName.toLowerCase().includes(d.name.toLowerCase()),
        )
        if (found) {
          matchedId = found.id
          isCustom = false
        } else {
          isCustom = true
        }
      }

      return {
        title: data.title || `Kế hoạch du lịch ${destName} ${days} ngày ${nights} đêm`,
        destinationId: matchedId,
        destinationName: destName,
        isCustomDestination: isCustom,
        customDestinationName: destName,
        customCountry: data.country || "Việt Nam",
        customLat: data.latitude,
        customLng: data.longitude,
        customTimezone: data.timezone,
        startDate: data.startDate || new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().split("T")[0],
        endDate: data.endDate || new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
        days,
        nights,
        travelers,
        budget,
        travelStyle: data.travelStyle || "BALANCED",
        childrenCount: data.childrenCount || 0,
        seniorsCount: data.seniorsCount || 0,
        hasVegetarian: !!data.hasVegetarian,
        hasMobilityDifficulty: !!data.hasMobilityDifficulty,
        summary: `Điểm đến: ${destName} • ${days} ngày ${nights} đêm • ${travelers} người • Ngân sách: ${new Intl.NumberFormat("vi-VN").format(budget)}đ`,
      }
    }
  } catch {
    // Nếu mạng chậm hoặc backend chưa phản hồi, chuyển sang bộ bóc tách động
  }

  // 2. Dự phòng động thuần ngữ pháp tiếng Việt (KHÔNG chứa bất kỳ danh sách cố định nào)
  return parseTripPromptDynamic(p, availableDestinations)
}

/**
 * Trích xuất động thuần ngữ pháp tiếng Việt, hoàn toàn không có danh sách fix cứng.
 */
export function parseTripPromptDynamic(
  prompt: string,
  availableDestinations: Array<{ id: number; name: string }> = [],
): ParsedTripPlan {
  const p = prompt.trim()
  const pLower = p.toLowerCase()

  // Bóc tách tên địa điểm theo cấu trúc: "đi <Địa điểm>", "đến <Địa điểm>", "tới <Địa điểm>"
  let extractedDest = ""
  const diMatch = p.match(/(?:đi|đến|tới|du lịch)\s+([a-zA-ZÀ-ỹ\s]+?)(?:\s+\d+\s*(?:ngày|n|đêm)|\s+trong|\s+cho|\s+cùng|\s+ngân|\s*$)/i)
  if (diMatch && diMatch[1].trim()) {
    const raw = diMatch[1].trim()
    if (!["đâu", "chơi", "nghỉ", "đó", "tour"].includes(raw.toLowerCase())) {
      extractedDest = raw.split(" ").map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(" ")
    }
  }

  // Tìm trong danh mục CSDL nhận từ backend nếu có
  let matchedDestId: number | null = null
  let isCustom = false

  if (extractedDest) {
    const found = availableDestinations.find(
      (d) => d.name.toLowerCase() === extractedDest.toLowerCase() ||
             pLower.includes(d.name.toLowerCase()),
    )
    if (found) {
      matchedDestId = found.id
      extractedDest = found.name
      isCustom = false
    } else {
      isCustom = true
    }
  } else {
    // Nếu câu không có từ "đi", thử so khớp với danh mục CSDL hiện có
    const found = availableDestinations.find((d) => pLower.includes(d.name.toLowerCase()))
    if (found) {
      matchedDestId = found.id
      extractedDest = found.name
      isCustom = false
    } else {
      extractedDest = "Điểm đến mong muốn"
      isCustom = true
    }
  }

  // Bóc tách số ngày & đêm
  let days = 3
  let nights = 2
  const matchDaysNights = p.match(/(\d+)\s*ngày\s*(\d+)\s*đêm/i) || p.match(/(\d+)\s*n\s*(\d+)\s*đ/i)
  if (matchDaysNights) {
    days = Number.parseInt(matchDaysNights[1], 10)
    nights = Number.parseInt(matchDaysNights[2], 10)
  } else {
    const matchDaysOnly = p.match(/(\d+)\s*ngày/i) || p.match(/(\d+)\s*n(?:gày)?/i)
    if (matchDaysOnly) {
      days = Number.parseInt(matchDaysOnly[1], 10)
      nights = Math.max(1, days - 1)
    } else if (pLower.includes("cuối tuần")) {
      days = 2
      nights = 1
    }
  }

  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
  const returnDate = new Date(tomorrow.getTime() + (days - 1) * 24 * 60 * 60 * 1000)
  const startDate = tomorrow.toISOString().split("T")[0]
  const endDate = returnDate.toISOString().split("T")[0]

  // Bóc tách số người
  let travelers = 2
  const matchTravelers = p.match(/(?:cho|cùng|\+)\s*(\d+)\s*(?:người|bạn|khách|thành viên)/i) || p.match(/(\d+)\s*(?:người|bạn|khách)/i)
  if (matchTravelers) {
    travelers = Number.parseInt(matchTravelers[1], 10)
  } else if (pLower.includes("một mình") || pLower.includes("solo") || pLower.includes("1 mình") || pLower.includes("1 người")) {
    travelers = 1
  } else if (pLower.includes("người yêu") || pLower.includes("vợ") || pLower.includes("chồng") || pLower.includes("cặp đôi") || pLower.includes("2 đứa")) {
    travelers = 2
  } else if (pLower.includes("gia đình")) {
    travelers = 4
  }

  // Bóc tách ngân sách
  let budget = Math.max(3000000, days * travelers * 800000)
  const matchTrieu = p.match(/(\d+(?:[.,]\d+)?)\s*(?:triệu|tr|củ)/i)
  if (matchTrieu) {
    budget = Math.round(Number.parseFloat(matchTrieu[1].replace(",", ".")) * 1000000)
  } else {
    const matchNumber = p.match(/(\d{1,3}(?:\.\d{3})+)\s*(?:đ|vnd|đồng)?/i)
    if (matchNumber) {
      budget = Number.parseInt(matchNumber[1].replace(/\./g, ""), 10)
    }
  }

  // Bóc tách nhịp độ
  let travelStyle: "RELAXED" | "BALANCED" | "FAST_PACED" = "BALANCED"
  if (pLower.includes("thư thả") || pLower.includes("chill") || pLower.includes("nghỉ dưỡng") || pLower.includes("nhẹ nhàng")) {
    travelStyle = "RELAXED"
  } else if (pLower.includes("trải nghiệm") || pLower.includes("khám phá") || pLower.includes("check in") || pLower.includes("nhiều điểm")) {
    travelStyle = "FAST_PACED"
  }

  // Bóc tách hồ sơ nhóm
  let childrenCount = 0
  let seniorsCount = 0
  let hasVegetarian = false
  let hasMobilityDifficulty = false

  if (pLower.includes("trẻ em") || pLower.includes("trẻ nhỏ") || pLower.includes("bé") || pLower.includes("con nhỏ")) {
    const matchKid = p.match(/(\d+)\s*(?:bé|trẻ|con)/i)
    childrenCount = matchKid ? Number.parseInt(matchKid[1], 10) : 1
  }
  if (pLower.includes("người cao tuổi") || pLower.includes("người già") || pLower.includes("ông bà") || pLower.includes("bố mẹ già")) {
    const matchOld = p.match(/(\d+)\s*(?:người già|ông bà)/i)
    seniorsCount = matchOld ? Number.parseInt(matchOld[1], 10) : 1
  }
  if (pLower.includes("ăn chay") || pLower.includes("quán chay") || pLower.includes("món chay") || pLower.includes("chay")) {
    hasVegetarian = true
  }
  if (pLower.includes("khó đi lại") || pLower.includes("ngại đi bộ") || pLower.includes("hạn chế đi xa") || pLower.includes("đau chân")) {
    hasMobilityDifficulty = true
  }

  const title = `Kế hoạch du lịch ${extractedDest} ${days} ngày ${nights} đêm`
  const summary = `Điểm đến: ${extractedDest} • ${days} ngày ${nights} đêm • ${travelers} người • Ngân sách: ${new Intl.NumberFormat("vi-VN").format(budget)}đ`

  return {
    title,
    destinationId: matchedDestId,
    destinationName: extractedDest,
    isCustomDestination: isCustom,
    customDestinationName: extractedDest,
    customCountry: "Việt Nam",
    startDate,
    endDate,
    days,
    nights,
    travelers,
    budget,
    travelStyle,
    childrenCount,
    seniorsCount,
    hasVegetarian,
    hasMobilityDifficulty,
    summary,
  }
}
