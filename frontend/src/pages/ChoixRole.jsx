import { useState } from "react"
import { GraduationCap, UserRound, ClipboardCheck, ArrowRight, Clock3, ShieldCheck } from "lucide-react"
import { Badge } from "@/components/ui/badge"

const ROLES = [
  {
    id: "formateur",
    titre: "Formateur",
    description: "Ouvrir une session, ajouter des présences, suivre le tableau et clôturer.",
    icone: GraduationCap,
    gradient: "from-amber-500/20 via-orange-500/10 to-transparent",
    accent: "text-amber-600 dark:text-amber-400",
    ring: "ring-amber-400",
    icon_bg: "bg-gradient-to-br from-amber-500 to-orange-500",
  },
  {
    id: "etudiant",
    titre: "Étudiant",
    description: "Marquer ma présence, déposer mon exercice et consulter ma note.",
    icone: UserRound,
    gradient: "from-emerald-500/20 via-teal-500/10 to-transparent",
    accent: "text-emerald-600 dark:text-emerald-400",
    ring: "ring-emerald-400",
    icon_bg: "bg-gradient-to-br from-emerald-500 to-teal-500",
  },
  {
    id: "relecteur",
    titre: "Relecteur",
    description: "Relire les exercices assignés et rendre une note sur 20.",
    icone: ClipboardCheck,
    gradient: "from-violet-500/20 via-purple-500/10 to-transparent",
    accent: "text-violet-600 dark:text-violet-400",
    ring: "ring-violet-400",
    icon_bg: "bg-gradient-to-br from-violet-500 to-purple-500",
  },
]

export default function ChoixRole({ onChoisir, darkToggle }) {
  const [role, setRole] = useState(null)

  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden bg-gradient-animated"
      data-testid="page-choix-role"
    >
      {/* Orbes décoratifs */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div className="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-blue-400/10 blur-3xl dark:bg-blue-400/5" />
        <div className="absolute -right-32 -bottom-32 h-96 w-96 rounded-full bg-violet-400/10 blur-3xl dark:bg-violet-400/5" />
        <div className="absolute left-1/2 top-1/3 h-64 w-64 -translate-x-1/2 rounded-full bg-emerald-400/10 blur-3xl dark:bg-emerald-400/5" />
      </div>

      {/* Bouton dark mode en haut à droite */}
      {darkToggle && (
        <div className="absolute right-4 top-4">{darkToggle}</div>
      )}

      <div className="relative w-full max-w-4xl px-4 py-12">

        {/* En-tête */}
        <div className="mb-12 text-center animate-fade-in">
          <Badge variant="secondary" className="mb-4 glass">
            KFOKAM48 — Épreuve 209
          </Badge>
          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl bg-gradient-to-r from-foreground to-muted-foreground bg-clip-text">
            Bienvenue
          </h1>
          <p className="mx-auto mt-3 max-w-xl text-balance text-muted-foreground">
            Choisissez votre rôle pour accéder à votre espace.
            Vous pouvez revenir à cette page à tout moment.
          </p>
        </div>

        {/* Grille des rôles */}
        <div className="grid gap-5 sm:grid-cols-3 stagger animate-slide-in-up" data-testid="grille-roles">
          {ROLES.map(({ id, titre, description, icone: Icone, gradient, accent, ring, icon_bg }) => {
            const selectionne = role === id
            return (
              <button
                key={id}
                type="button"
                data-testid={`role-${id}`}
                aria-pressed={selectionne}
                onClick={() => setRole(id)}
                onDoubleClick={() => onChoisir(id)}
                className={[
                  "group relative rounded-2xl border bg-card/80 backdrop-blur-sm",
                  `bg-gradient-to-br ${gradient}`,
                  "p-7 text-left shadow-sm transition-all duration-300",
                  "hover:-translate-y-2 hover:shadow-xl",
                  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                  selectionne
                    ? `border-ring ring-2 ${ring} shadow-lg -translate-y-1 animate-pulse-ring`
                    : "border-border hover:border-ring/50",
                ].join(" ")}
              >
                {/* Icône */}
                <div className={`inline-flex h-12 w-12 items-center justify-center rounded-xl ${icon_bg} text-white shadow-md`}>
                  <Icone className="h-6 w-6" />
                </div>

                <h2 className="mt-5 text-lg font-semibold">{titre}</h2>
                <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">{description}</p>

                <span
                  className={[
                    "mt-5 inline-flex items-center gap-1.5 text-sm font-medium",
                    accent,
                    "opacity-0 transition-all duration-200 group-hover:opacity-100 group-hover:gap-2",
                    selectionne ? "opacity-100 gap-2" : "",
                  ].join(" ")}
                >
                  Continuer <ArrowRight className="h-4 w-4" />
                </span>

                {selectionne && (
                  <span className="absolute right-3 top-3 rounded-full bg-primary px-2.5 py-1 text-xs font-semibold text-primary-foreground shadow-sm">
                    ✓ Sélectionné
                  </span>
                )}
              </button>
            )
          })}
        </div>

        {/* Bouton continuer */}
        <div className="mt-12 flex justify-center animate-fade-in" style={{ animationDelay: "300ms" }}>
          <button
            type="button"
            data-testid="btn-continuer"
            disabled={!role}
            onClick={() => role && onChoisir(role)}
            className={[
              "inline-flex items-center gap-2.5 rounded-xl px-10 py-3.5 text-sm font-semibold",
              "bg-primary text-primary-foreground shadow-lg",
              "transition-all duration-200 hover:bg-primary/90 hover:shadow-xl hover:-translate-y-0.5",
              "disabled:pointer-events-none disabled:opacity-40 disabled:translate-y-0",
            ].join(" ")}
          >
            Accéder à mon espace <ArrowRight className="h-4 w-4" />
          </button>
        </div>

        {/* Badges informatifs */}
        <div
          className="mt-10 flex flex-wrap items-center justify-center gap-x-8 gap-y-2 text-xs text-muted-foreground animate-fade-in"
          style={{ animationDelay: "400ms" }}
        >
          <span className="inline-flex items-center gap-1.5">
            <Clock3 className="h-3.5 w-3.5" /> Codes de présence valables 15 minutes
          </span>
          <span className="inline-flex items-center gap-1.5">
            <ShieldCheck className="h-3.5 w-3.5" /> Relecteur toujours anonyme
          </span>
        </div>
      </div>
    </div>
  )
}
