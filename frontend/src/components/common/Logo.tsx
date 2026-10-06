import { Compass } from "lucide-react"

export function Logo({ className = "h-7 w-7" }: { className?: string }) {
  return (
    <div className="flex items-center gap-2.5 font-bold text-lg tracking-tight select-none">
      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 text-white shadow-md shadow-blue-500/20">
        <Compass className={className} />
      </div>
      <span className="bg-gradient-to-r from-blue-600 via-indigo-600 to-violet-600 bg-clip-text text-transparent dark:from-blue-400 dark:to-indigo-300">
        TripMind<span className="text-foreground text-sm font-medium ml-1 px-1.5 py-0.5 rounded-md bg-muted">AI</span>
      </span>
    </div>
  )
}
