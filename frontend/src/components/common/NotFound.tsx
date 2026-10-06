import { Link } from "@tanstack/react-router"
import { Button } from "@/components/ui/button"
import { Compass } from "lucide-react"

export function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <div className="p-4 rounded-full bg-primary/10 text-primary mb-4">
        <Compass className="h-10 w-10 animate-spin" style={{ animationDuration: "12s" }} />
      </div>
      <h2 className="text-2xl font-bold tracking-tight mb-2">Không tìm thấy trang</h2>
      <p className="text-muted-foreground text-sm max-w-md mb-6">
        Điểm đến bạn đang tìm không tồn tại hoặc đã được chuyển sang hành trình khác.
      </p>
      <Link to="/">
        <Button>Trở về trang chủ</Button>
      </Link>
    </div>
  )
}
