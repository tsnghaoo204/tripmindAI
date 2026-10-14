import type { ChecklistItem } from "@/client/types"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { CheckSquare, Square, Trash2 } from "lucide-react"

interface ChecklistColumnProps {
  icon: React.ReactNode
  title: string
  doneLabel: string
  emptyText: string
  items: ChecklistItem[]
  onToggle: (item: ChecklistItem) => void
  onDelete: (item: ChecklistItem) => void
}

export function ChecklistColumn({
  icon,
  title,
  doneLabel,
  emptyText,
  items,
  onToggle,
  onDelete,
}: ChecklistColumnProps) {
  return (
    <Card className="p-5 space-y-3">
      <div className="flex items-center justify-between border-b pb-2">
        <div className="flex items-center gap-2 font-bold text-sm text-foreground">
          {icon}
          <span>
            {title} ({items.length})
          </span>
        </div>
        <span className="text-[11px] text-muted-foreground">
          {items.filter((i) => i.done).length} {doneLabel}
        </span>
      </div>

      {items.length === 0 ? (
        <p className="text-xs text-muted-foreground py-6 text-center">{emptyText}</p>
      ) : (
        <ul className="divide-y divide-border/60">
          {items.map((item) => (
            <li key={item.id} className="py-2.5 flex items-center justify-between gap-3 text-xs group">
              <button
                type="button"
                aria-pressed={item.done}
                onClick={() => onToggle(item)}
                className="flex items-center gap-2.5 text-left flex-1 cursor-pointer"
              >
                {item.done ? (
                  <CheckSquare className="h-4 w-4 text-emerald-500 shrink-0" aria-hidden />
                ) : (
                  <Square className="h-4 w-4 text-muted-foreground shrink-0" aria-hidden />
                )}
                <div className="space-y-0.5">
                  <span className={item.done ? "line-through text-muted-foreground" : "text-foreground font-medium"}>
                    {item.title}
                  </span>
                  {item.dueDate && (
                    <span className="block text-[10px] text-muted-foreground">Hạn chót: {item.dueDate}</span>
                  )}
                </div>
              </button>

              <Button
                size="icon"
                variant="ghost"
                onClick={() => onDelete(item)}
                className="h-6 w-6 opacity-0 group-hover:opacity-100 focus-visible:opacity-100 transition-opacity text-muted-foreground hover:text-destructive"
                aria-label={`Xoá ${item.title}`}
              >
                <Trash2 className="h-3 w-3" />
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
