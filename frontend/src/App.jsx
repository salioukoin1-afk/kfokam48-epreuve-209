import { useEffect, useState } from "react"
import { GraduationCap, BookOpenCheck, ClipboardCheck, ArrowLeft, Sun, Moon } from "lucide-react"
import ChoixRole from "./pages/ChoixRole.jsx"
import EcranFormateur from "./pages/Formateur.jsx"
import EcranEtudiant from "./pages/Etudiant.jsx"
import EcranRelecteur from "./pages/Relecteur.jsx"
import { Button } from "@/components/ui/button"

const ENTETES = {
  formateur: { titre: "Espace formateur", icone: GraduationCap, teinte: "from-amber-500 to-orange-500" },
  etudiant:  { titre: "Espace étudiant",  icone: BookOpenCheck,  teinte: "from-emerald-500 to-teal-500" },
  relecteur: { titre: "Espace relecteur", icone: ClipboardCheck, teinte: "from-violet-500 to-purple-500" },
}

function useDarkMode() {
  const [dark, setDark] = useState(() => {
    try {
      return localStorage.getItem("theme") === "dark"
    } catch {
      return false
    }
  })

  useEffect(() => {
    const root = document.documentElement
    if (dark) {
      root.classList.add("dark")
      localStorage.setItem("theme", "dark")
    } else {
      root.classList.remove("dark")
      localStorage.setItem("theme", "light")
    }
  }, [dark])

  return [dark, setDark]
}

export default function App() {
  const [role, setRole] = useState(null)
  const [dark, setDark] = useDarkMode()

  const DarkToggle = () => (
    <Button
      variant="ghost"
      size="sm"
      onClick={() => setDark((d) => !d)}
      data-testid="toggle-dark"
      aria-label={dark ? "Passer en mode clair" : "Passer en mode sombre"}
      className="rounded-full"
    >
      {dark
        ? <Sun  className="h-4 w-4 text-amber-400" />
        : <Moon className="h-4 w-4 text-slate-600" />}
    </Button>
  )

  if (!role) {
    return <ChoixRole onChoisir={setRole} darkToggle={<DarkToggle />} />
  }

  const entete = ENTETES[role]
  const Icone = entete.icone

  return (
    <div className="min-h-screen">
      <header className="border-b bg-card shadow-sm">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <div className={`inline-flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br ${entete.teinte} text-white shadow-md`}>
              <Icone className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-semibold leading-tight">{entete.titre}</p>
              <p className="text-xs text-muted-foreground">KFOKAM48 · matricule 209</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <DarkToggle />
            <Button variant="ghost" size="sm" onClick={() => setRole(null)} data-testid="changer-role">
              <ArrowLeft className="h-4 w-4" /> Changer de rôle
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8 animate-fade-in">
        {role === "formateur" && <EcranFormateur />}
        {role === "etudiant"  && <EcranEtudiant />}
        {role === "relecteur" && <EcranRelecteur />}
      </main>
    </div>
  )
}
