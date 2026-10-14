import { getErrorMessage, isForbidden } from "@/client/errors"
import { Card } from "@/components/ui/card"
import { ShieldAlert } from "lucide-react"

/** Shows why an admin query failed; 403 gets an explicit permission message. */
export function AdminQueryError({ error }: { error: unknown }) {
  return (
    <Card className="p-8 text-center text-sm text-muted-foreground space-y-2" role="alert">
      <ShieldAlert className="h-8 w-8 mx-auto text-destructive" aria-hidden />
      <p className="font-semibold text-foreground">
        {isForbidden(error) ? "Bạn không có quyền truy cập dữ liệu quản trị này" : "Không thể tải dữ liệu quản trị"}
      </p>
      <p className="text-xs">{getErrorMessage(error)}</p>
    </Card>
  )
}
