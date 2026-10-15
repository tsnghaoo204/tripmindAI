import { placeApi } from "@/client/api/place-api"
import { getErrorMessage } from "@/client/errors"
import type { Place } from "@/client/types"
import { type MapPoint, TripMap } from "@/components/common/TripMap"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute } from "@tanstack/react-router"
import {
  Bookmark,
  Coffee,
  Compass,
  ExternalLink,
  Heart,
  LayoutGrid,
  Loader2,
  Map as MapIcon,
  MapPin,
  Search,
  Sparkles,
  Star,
  Utensils,
} from "lucide-react"
import { useMemo, useState } from "react"
import { toast } from "sonner"

export const Route = createFileRoute("/_layout/places")({
  component: PlacesPage,
})

const POPULAR_DESTINATIONS = [
  { id: "ALL", label: "Tất cả" },
  { id: "Da Nang", label: "Đà Nẵng" },
  { id: "Nha Trang", label: "Nha Trang" },
  { id: "Hà Nội", label: "Hà Nội" },
  { id: "TP. Hồ Chí Minh", label: "TP. HCM" },
  { id: "Phú Quốc", label: "Phú Quốc" },
  { id: "Hội An", label: "Hội An" },
  { id: "Đà Lạt", label: "Đà Lạt" },
]

function getCategoryBadge(category?: string | null) {
  const cat = (category || "").toUpperCase()
  if (cat.includes("FOOD") || cat.includes("RESTAURANT") || cat.includes("AN_UONG")) {
    return {
      label: "Ẩm thực",
      icon: Utensils,
      className: "bg-orange-500/10 text-orange-600 dark:text-orange-400 border-orange-500/20",
    }
  }
  if (cat.includes("CAFE") || cat.includes("COFFEE")) {
    return {
      label: "Cà phê",
      icon: Coffee,
      className: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20",
    }
  }
  if (cat.includes("BEACH")) {
    return {
      label: "Bãi biển",
      icon: Compass,
      className: "bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20",
    }
  }
  return { label: "Tham quan", icon: Compass, className: "bg-primary/10 text-primary border-primary/20" }
}

function PlacesPage() {
  const queryClient = useQueryClient()
  const [searchInput, setSearchInput] = useState("")
  const [queryText, setQueryText] = useState("")
  const [selectedDestination, setSelectedDestination] = useState<string>("ALL")
  const [activeTab, setActiveTab] = useState<string>("explore")
  const [viewMode, setViewMode] = useState<"grid" | "map">("grid")
  const [selectedMapPointId, setSelectedMapPointId] = useState<number | string | null>(null)

  // Query: Search places
  const effectiveQuery = queryText || (selectedDestination !== "ALL" ? selectedDestination : "Đà Nẵng")
  const destinationParam = selectedDestination !== "ALL" ? selectedDestination : undefined

  const {
    data: searchResults = [],
    isLoading: isSearching,
    error: searchError,
  } = useQuery({
    queryKey: ["places", "search", effectiveQuery, destinationParam],
    queryFn: () => placeApi.search(effectiveQuery, destinationParam),
  })

  // Query: Saved places
  const { data: savedPlaces = [], isLoading: isSavedLoading } = useQuery({
    queryKey: ["saved-places"],
    queryFn: () => placeApi.getSavedPlaces(),
  })

  const savedPlaceIds = useMemo(() => {
    const map = new Map<number, number>()
    for (const sp of savedPlaces) {
      if (sp.place?.id) {
        map.set(sp.place.id, sp.id)
      }
    }
    return map
  }, [savedPlaces])

  const exploreMapPoints = useMemo(() => {
    return searchResults
      .filter(
        (p) => p.latitude != null && p.longitude != null && !Number.isNaN(p.latitude) && !Number.isNaN(p.longitude),
      )
      .map((p, idx) => ({
        id: p.id,
        name: p.name,
        latitude: Number(p.latitude),
        longitude: Number(p.longitude),
        address: p.address,
        category: p.category || p.primaryType,
        orderIndex: idx,
        rating: p.rating,
      }))
  }, [searchResults])

  const savedMapPoints = useMemo(() => {
    return savedPlaces
      .filter(
        (sp) =>
          sp.place?.latitude != null &&
          sp.place?.longitude != null &&
          !Number.isNaN(sp.place.latitude) &&
          !Number.isNaN(sp.place.longitude),
      )
      .map((sp, idx) => ({
        id: sp.place.id,
        name: sp.place.name,
        latitude: Number(sp.place.latitude),
        longitude: Number(sp.place.longitude),
        address: sp.place.address,
        category: sp.place.category || sp.place.primaryType,
        orderIndex: idx,
        rating: sp.place.rating,
      }))
  }, [savedPlaces])

  // Save place mutation
  const saveMutation = useMutation({
    mutationFn: (placeId: number) => placeApi.savePlace(placeId),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ["saved-places"] })
      toast.success(`Đã lưu "${saved.place?.name || "địa điểm"}" vào danh sách yêu thích!`)
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể lưu địa điểm")),
  })

  // Unsave place mutation
  const unsaveMutation = useMutation({
    mutationFn: (placeId: number) => placeApi.unsavePlace(placeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["saved-places"] })
      toast.success("Đã xóa khỏi danh sách yêu thích")
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể bỏ lưu địa điểm")),
  })

  const toggleSave = (place: Place) => {
    if (savedPlaceIds.has(place.id)) {
      unsaveMutation.mutate(place.id)
    } else {
      saveMutation.mutate(place.id)
    }
  }

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setQueryText(searchInput.trim())
  }

  const renderPlaceCard = (place: Place, isSaved: boolean) => {
    const cat = getCategoryBadge(place.category || place.primaryType)
    const CatIcon = cat.icon
    const isBusy =
      (saveMutation.isPending && saveMutation.variables === place.id) ||
      (unsaveMutation.isPending && unsaveMutation.variables === place.id)

    const mapSearchUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
      `${place.name} ${place.address || ""}`.trim(),
    )}`

    return (
      <Card
        key={place.id}
        className="overflow-hidden hover:shadow-lg transition-all duration-300 border-border/80 flex flex-col group bg-card"
      >
        <div className="h-44 relative bg-gradient-to-br from-primary/10 via-muted to-muted/60 overflow-hidden flex items-center justify-center">
          {/* Decorative pattern */}
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_bottom_left,rgba(59,130,246,0.1),transparent_50%)]" />

          <div className="flex flex-col items-center justify-center gap-2 text-muted-foreground/60 group-hover:scale-105 transition-transform duration-300">
            <CatIcon className="h-10 w-10 text-primary/40" />
            <span className="text-xs font-medium tracking-wide uppercase">{cat.label}</span>
          </div>

          {/* Category badge */}
          <div className="absolute bottom-3 left-3">
            <Badge
              variant="outline"
              className={`text-xs px-2.5 py-0.5 backdrop-blur-md bg-background/80 ${cat.className}`}
            >
              <CatIcon className="h-3 w-3 mr-1" />
              {cat.label}
            </Badge>
          </div>

          {/* Heart bookmark button */}
          <div className="absolute top-3 right-3">
            <Button
              type="button"
              aria-label={isSaved ? "Bỏ lưu địa điểm" : "Lưu địa điểm"}
              size="icon"
              variant="secondary"
              onClick={() => toggleSave(place)}
              disabled={isBusy}
              className={`h-9 w-9 rounded-full shadow-md backdrop-blur-md transition-transform active:scale-95 cursor-pointer ${
                isSaved
                  ? "bg-rose-50 text-rose-500 hover:bg-rose-100 dark:bg-rose-950/60 dark:hover:bg-rose-900/80"
                  : "bg-background/80 hover:bg-background text-muted-foreground hover:text-foreground"
              }`}
            >
              <Heart
                className={`h-4 w-4 transition-colors ${
                  isSaved ? "fill-rose-500 text-rose-500" : "text-muted-foreground"
                }`}
              />
            </Button>
          </div>
        </div>

        <CardContent className="p-4 flex-1 flex flex-col justify-between gap-3">
          <div>
            {/* Rating & reviews */}
            <div className="flex items-center gap-1.5 text-xs text-amber-500 mb-1.5">
              <Star className="h-3.5 w-3.5 fill-current" />
              <span className="font-bold text-foreground">
                {place.rating != null ? Number(place.rating).toFixed(1) : "4.5"}
              </span>
              <span className="text-muted-foreground">
                ({place.userRatingsTotal ? place.userRatingsTotal.toLocaleString() : "100+"} đánh giá)
              </span>
            </div>

            <h3 className="font-bold text-base line-clamp-1 group-hover:text-primary transition-colors">
              {place.name}
            </h3>

            {place.address && (
              <p className="text-xs text-muted-foreground flex items-start gap-1 mt-1.5 line-clamp-2">
                <MapPin className="h-3.5 w-3.5 text-primary shrink-0 mt-0.5" />
                <span>{place.address}</span>
              </p>
            )}
          </div>

          <div className="pt-2 border-t border-border/50 flex items-center justify-between gap-2">
            <span className="text-[11px] text-muted-foreground font-mono">
              {place.provider === "GOOGLE" ? "Google Places" : "TripMind Catalog"}
            </span>

            <a
              href={mapSearchUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-xs text-primary hover:underline font-medium"
            >
              <span>Xem trên bản đồ</span>
              <ExternalLink className="h-3 w-3" />
            </a>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8 max-w-6xl space-y-8">
      {/* Hero Banner */}
      <div className="relative rounded-3xl p-6 sm:p-10 overflow-hidden bg-gradient-to-br from-primary/10 via-sky-500/10 to-background border border-border/70 shadow-sm text-center max-w-3xl mx-auto">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-primary/10 text-primary border border-primary/20 mb-3">
          <Sparkles className="h-3.5 w-3.5" aria-hidden />
          <span>Kho Dữ Liệu Điểm Đến Thông Minh</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight mb-2 bg-clip-text text-transparent bg-gradient-to-r from-foreground to-foreground/70">
          Khám Phá Địa Điểm
        </h1>
        <p className="text-muted-foreground text-sm max-w-xl mx-auto">
          Tìm kiếm quán ăn đặc sản, danh lam thắng cảnh và địa điểm nổi tiếng. Lưu lại để trợ lý AI tự động gợi ý vào
          lịch trình của bạn.
        </p>

        {/* Search bar */}
        <form onSubmit={handleSearchSubmit} className="flex gap-2 mt-6 max-w-xl mx-auto">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-2.5 h-4 w-4 text-muted-foreground" aria-hidden />
            <Input
              aria-label="Tìm địa điểm"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Nhập tên quán cafe, bãi biển, đền chùa hoặc món ăn..."
              className="pl-10 h-11 bg-background/90 text-sm shadow-sm"
            />
          </div>
          <Button type="submit" className="h-11 px-5 gap-2 font-semibold shadow-md shadow-primary/20">
            <Search className="h-4 w-4" />
            <span className="hidden sm:inline">Tìm kiếm</span>
          </Button>
        </form>

        {/* Popular Destination chips */}
        <div className="flex flex-wrap items-center justify-center gap-1.5 mt-4">
          <span className="text-xs text-muted-foreground mr-1">Điểm đến:</span>
          {POPULAR_DESTINATIONS.map((dest) => (
            <button
              type="button"
              key={dest.id}
              onClick={() => {
                setSelectedDestination(dest.id)
                setQueryText("")
              }}
              className={`px-3 py-1 rounded-full text-xs font-medium transition-all cursor-pointer ${
                selectedDestination === dest.id
                  ? "bg-primary text-primary-foreground shadow-sm shadow-primary/30"
                  : "bg-muted/80 text-muted-foreground hover:bg-muted hover:text-foreground"
              }`}
            >
              {dest.label}
            </button>
          ))}
        </div>
      </div>

      {/* Main Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b pb-3">
          <div className="flex items-center gap-3">
            <TabsList>
              <TabsTrigger value="explore" className="gap-2">
                <Compass className="h-4 w-4" />
                Khám phá ({searchResults.length})
              </TabsTrigger>
              <TabsTrigger value="saved" className="gap-2">
                <Bookmark className="h-4 w-4" />
                Đã lưu ({savedPlaces.length})
              </TabsTrigger>
            </TabsList>

            <span className="text-xs text-muted-foreground hidden md:inline-block">
              {activeTab === "explore"
                ? `Kết quả cho "${effectiveQuery}"`
                : `${savedPlaces.length} địa điểm trong danh sách yêu thích`}
            </span>
          </div>

          <div className="flex items-center gap-1 self-start sm:self-auto bg-muted/70 p-1 rounded-xl border border-border/50">
            <Button
              type="button"
              size="sm"
              variant={viewMode === "grid" ? "default" : "ghost"}
              className="h-8 px-3 text-xs gap-1.5 rounded-lg"
              onClick={() => setViewMode("grid")}
            >
              <LayoutGrid className="h-3.5 w-3.5" /> Dạng lưới
            </Button>
            <Button
              type="button"
              size="sm"
              variant={viewMode === "map" ? "default" : "ghost"}
              className="h-8 px-3 text-xs gap-1.5 rounded-lg"
              onClick={() => setViewMode("map")}
            >
              <MapIcon className="h-3.5 w-3.5" /> Dạng bản đồ
            </Button>
          </div>
        </div>

        {/* Tab 1: Explore */}
        <TabsContent value="explore" className="space-y-6 mt-0">
          {isSearching ? (
            <div className="py-20 text-center space-y-3">
              <Loader2 className="h-8 w-8 animate-spin mx-auto text-primary" />
              <p className="text-sm text-muted-foreground">Đang tra cứu danh sách địa điểm...</p>
            </div>
          ) : searchError ? (
            <div className="py-16 text-center text-sm text-destructive">
              {getErrorMessage(searchError, "Lỗi khi tìm kiếm địa điểm.")}
            </div>
          ) : searchResults.length === 0 ? (
            <div className="py-16 text-center space-y-3 bg-muted/20 rounded-2xl border border-dashed border-border p-8">
              <div className="h-12 w-12 rounded-full bg-muted flex items-center justify-center mx-auto text-muted-foreground">
                <Search className="h-6 w-6" />
              </div>
              <p className="font-semibold text-base">Không tìm thấy địa điểm nào khớp với từ khóa</p>
              <p className="text-xs text-muted-foreground max-w-sm mx-auto">
                Hãy thử đổi từ khóa tìm kiếm (ví dụ: &ldquo;Cầu Rồng&rdquo;, &ldquo;Bãi biển Mỹ Khê&rdquo;) hoặc chọn
                điểm đến khác.
              </p>
            </div>
          ) : viewMode === "map" ? (
            exploreMapPoints.length === 0 ? (
              <div className="py-16 text-center space-y-2 bg-muted/20 rounded-2xl border border-dashed border-border p-8">
                <MapIcon className="h-8 w-8 text-muted-foreground mx-auto opacity-50" />
                <p className="font-semibold text-sm">Chưa có toạ độ địa lý cho các địa điểm tìm kiếm này</p>
                <p className="text-xs text-muted-foreground">
                  Vui lòng chuyển lại Dạng lưới để xem danh sách chi tiết.
                </p>
              </div>
            ) : (
              <div className="rounded-2xl overflow-hidden border border-border shadow-md">
                <div className="bg-muted/40 px-4 py-2.5 border-b border-border flex items-center justify-between text-xs">
                  <span className="font-semibold flex items-center gap-1.5">
                    <MapIcon className="h-3.5 w-3.5 text-primary" /> Bản đồ {exploreMapPoints.length} địa điểm tại{" "}
                    {effectiveQuery}
                  </span>
                  <span className="text-muted-foreground text-[11px] hidden sm:inline">
                    Bấm vào từng ghim để xem thông tin chi tiết
                  </span>
                </div>
                <TripMap
                  points={exploreMapPoints}
                  height="h-[520px]"
                  selectedPointId={selectedMapPointId}
                  onSelectPoint={(p: MapPoint) => setSelectedMapPointId(p.id)}
                  showRoute={false}
                />
              </div>
            )
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {searchResults.map((p) => renderPlaceCard(p, savedPlaceIds.has(p.id)))}
            </div>
          )}
        </TabsContent>

        {/* Tab 2: Saved Places */}
        <TabsContent value="saved" className="space-y-6 mt-0">
          {isSavedLoading ? (
            <div className="py-20 text-center space-y-3">
              <Loader2 className="h-8 w-8 animate-spin mx-auto text-primary" />
              <p className="text-sm text-muted-foreground">Đang tải danh sách địa điểm đã lưu...</p>
            </div>
          ) : savedPlaces.length === 0 ? (
            <div className="py-16 text-center space-y-3 bg-muted/20 rounded-2xl border border-dashed border-border p-8">
              <div className="h-12 w-12 rounded-full bg-rose-500/10 text-rose-500 flex items-center justify-center mx-auto">
                <Heart className="h-6 w-6" />
              </div>
              <p className="font-semibold text-base">Bạn chưa lưu địa điểm nào</p>
              <p className="text-xs text-muted-foreground max-w-sm mx-auto">
                Khi duyệt danh mục hoặc tìm kiếm, hãy bấm biểu tượng trái tim để lưu lại những nơi bạn muốn ghé thăm
                nhé!
              </p>
              <Button variant="outline" size="sm" onClick={() => setActiveTab("explore")}>
                Khám phá ngay
              </Button>
            </div>
          ) : viewMode === "map" ? (
            savedMapPoints.length === 0 ? (
              <div className="py-16 text-center space-y-2 bg-muted/20 rounded-2xl border border-dashed border-border p-8">
                <MapIcon className="h-8 w-8 text-muted-foreground mx-auto opacity-50" />
                <p className="font-semibold text-sm">Chưa có toạ độ địa lý cho các địa điểm đã lưu</p>
                <p className="text-xs text-muted-foreground">
                  Vui lòng chuyển lại Dạng lưới để xem danh sách chi tiết.
                </p>
              </div>
            ) : (
              <div className="rounded-2xl overflow-hidden border border-border shadow-md">
                <div className="bg-muted/40 px-4 py-2.5 border-b border-border flex items-center justify-between text-xs">
                  <span className="font-semibold flex items-center gap-1.5">
                    <MapIcon className="h-3.5 w-3.5 text-primary" /> Bản đồ {savedMapPoints.length} địa điểm đã lưu
                  </span>
                  <span className="text-muted-foreground text-[11px] hidden sm:inline">
                    Bấm vào từng ghim để xem thông tin chi tiết
                  </span>
                </div>
                <TripMap
                  points={savedMapPoints}
                  height="h-[520px]"
                  selectedPointId={selectedMapPointId}
                  onSelectPoint={(p: MapPoint) => setSelectedMapPointId(p.id)}
                  showRoute={false}
                />
              </div>
            )
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {savedPlaces.map((sp) => renderPlaceCard(sp.place, true))}
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
