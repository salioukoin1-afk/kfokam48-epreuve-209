import { AlertTriangle, Loader2, RotateCcw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/** F3 : états de chargement et d'erreur explicites sur chaque écran. */
export default function Etat({ chargement, erreur, onRetry, children, className }) {
  if (chargement) {
    return (
      <div role="status" className={cn("flex items-center justify-center gap-2 rounded-lg border bg-card p-8 text-muted-foreground", className)}>
        <Loader2 className="h-4 w-4 animate-spin" /> Chargement…
      </div>
    )
  }
  if (erreur) {
    return (
      <div role="alert" className={cn("rounded-lg border border-destructive/40 bg-destructive/5 p-4", className)}>
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-destructive" />
          <div className="min-w-0">
            <p className="font-semibold text-destructive">{erreur.code}</p>
            <p className="text-sm text-muted-foreground">{erreur.message}</p>
            {onRetry && (
              <Button variant="outline" size="sm" className="mt-3" onClick={onRetry}>
                <RotateCcw className="h-4 w-4" /> Réessayer
              </Button>
            )}
          </div>
        </div>
      </div>
    )
  }
  return children || null
}

/** Message de succès réutilisable (retour visuel des actions). */
export function Succes({ children }) {
  if (!children) return null
  return (
    <div role="status" className="rounded-lg border border-emerald-500/40 bg-emerald-500/5 p-3 text-sm font-medium text-emerald-700 dark:text-emerald-400">
      {children}
    </div>
  )
}
