import { createFileRoute } from "@tanstack/react-router"
import { useState } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Search, MapPin, Star, Heart } from "lucide-react"

export const Route = createFileRoute("/_layout/places")({
  component: PlacesPage,
})

function PlacesPage() {
  const [query, setQuery] = useState("")

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8">
      <div className="max-w-2xl mx-auto text-center mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight mb-2">Khám Phá Địa Điểm</h1>
        <p className="text-muted-foreground text-sm">
          Tìm kiếm quán ăn, danh lam thắng cảnh và địa điểm lưu trú hỗ trợ bởi Google Places
        </p>

        <div className="flex gap-2 mt-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Nhập tên quán cafe, địa danh hoặc bãi biển..."
              className="pl-9"
            />
          </div>
          <Button>Tìm kiếm</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="overflow-hidden hover:shadow-md transition-shadow">
          <div className="h-40 bg-muted relative">
            <div className="absolute top-3 right-3">
              <Button size="icon" variant="secondary" className="h-8 w-8 rounded-full bg-white/80 dark:bg-black/50 backdrop-blur-sm">
                <Heart className="h-4 w-4 text-rose-500" />
              </Button>
            </div>
          </div>
          <CardContent className="p-4">
            <div className="flex items-center gap-1 text-xs text-amber-500 mb-1">
              <Star className="h-3.5 w-3.5 fill-current" />
              <span className="font-semibold">4.8</span>
              <span className="text-muted-foreground">(1,240 đánh giá)</span>
            </div>
            <h3 className="font-bold text-base">Cầu Rồng Đà Nẵng</h3>
            <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
              <MapPin className="h-3 w-3" /> An Hải Tây, Sơn Trà, Đà Nẵng
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
