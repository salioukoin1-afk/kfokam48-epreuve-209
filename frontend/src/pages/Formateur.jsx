import { useCallback, useEffect, useState } from "react"
import { GraduationCap, ArrowLeft, RefreshCw, PlayCircle, StopCircle, LayoutDashboard, UserPlus } from "lucide-react"
import Etat, { Succes } from "@/components/Etat.jsx"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label, Select } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { sessionsApi, tableauApi, presencesApi, etudiantsApi } from "@/api/modules.js"

const PROMOTION_DEMO = 1

export default function EcranFormateur() {
  // Session ouverte par le formateur pendant cette visite (état local).
  const [session, setSession] = useState(null)
  const [cloturee, setCloturee] = useState(false)
  const [confirmation, setConfirmation] = useState(false)
  const [titre, setTitre] = useState("")
  const [ouvertureEnCours, setOuvertureEnCours] = useState(false)
  const [erreurSession, setErreurSession] = useState(null)
  const [messageCloture, setMessageCloture] = useState("")

  // Présence manuelle
  const [etudiants, setEtudiants] = useState([])
  const [etudiantChoisi, setEtudiantChoisi] = useState("")
  const [messagePresence, setMessagePresence] = useState("")
  const [erreurPresence, setErreurPresence] = useState(null)

  // Tableau
  const [lignes, setLignes] = useState(null)
  const [chargementTableau, setChargementTableau] = useState(true)
  const [erreurTableau, setErreurTableau] = useState(null)

  const chargerTableau = useCallback(() => {
    setChargementTableau(true)
    setErreurTableau(null)
    tableauApi.consulter(PROMOTION_DEMO)
      .then(setLignes)
      .catch(setErreurTableau)
      .finally(() => setChargementTableau(false))
  }, [])

  useEffect(() => { chargerTableau() }, [chargerTableau])

  useEffect(() => {
    etudiantsApi.liste(PROMOTION_DEMO).then(setEtudiants).catch(() => setEtudiants([]))
  }, [])

  // Compte à rebours d'expiration du code (retour visuel RG2).
  const [maintenant, setMaintenant] = useState(Date.now())
  useEffect(() => {
    const t = setInterval(() => setMaintenant(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])

  const minutesRestantes = session && !cloturee
    ? Math.max(0, Math.floor((new Date(session.expirationAt).getTime() - maintenant) / 60000))
    : null

  const ouvrirSession = async (e) => {
    e.preventDefault()
    setErreurSession(null)
    setMessageCloture("")
    setOuvertureEnCours(true)
    try {
      const s = await sessionsApi.ouvrir(titre.trim(), PROMOTION_DEMO)
      setSession(s)
      setCloturee(false)
      setConfirmation(false)
      setTitre("")
    } catch (err) {
      setErreurSession(err)
    } finally {
      setOuvertureEnCours(false)
    }
  }

  const cloturer = async () => {
    setErreurSession(null)
    try {
      await sessionsApi.cloturer(session.id)
      setCloturee(true)
      setConfirmation(false)
      setMessageCloture("Session clôturée — notes verrouillées, dépôts refusés (RG13).")
    } catch (err) {
      setConfirmation(false)
      setErreurSession(err)
    }
  }

  const marquerPresent = async (e) => {
    e.preventDefault()
    setErreurPresence(null)
    setMessagePresence("")
    try {
      const p = await presencesApi.ajouterFormateur(session.id, Number(etudiantChoisi))
      setMessagePresence(`Présence enregistrée (source : ${p.source}) ✓`)
      setEtudiantChoisi("")
      chargerTableau()
    } catch (err) {
      setErreurPresence(err)
    }
  }

  return (
    <div className="space-y-6">
      {/* OUVERTURE DE SESSION */}
      <Card data-testid="carte-session">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <PlayCircle className="h-5 w-5 text-amber-600" /> Ouvrir une session
          </CardTitle>
          <CardDescription>
            Un code unique à 6 caractères est généré et expire au bout de 15 minutes (RG2).
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={ouvrirSession} className="flex max-w-xl gap-2">
            <Input
              data-testid="input-titre"
              value={titre}
              onChange={(e) => setTitre(e.target.value)}
              placeholder="Titre de la session (ex. TP 4 — API REST)"
              minLength={3}
              required
            />
            <Button type="submit" disabled={ouvertureEnCours} data-testid="btn-ouvrir">
              {ouvertureEnCours ? "Ouverture…" : "Ouvrir"}
            </Button>
          </form>
          <Etat chargement={false} erreur={erreurSession} />

          {session && (
            <div className="rounded-xl border bg-gradient-to-br from-amber-500/10 to-transparent p-6" data-testid="encart-code">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-muted-foreground">Code de présence à communiquer</p>
                  <p className="mt-1 font-mono text-5xl font-bold tracking-[0.3em] text-amber-700 dark:text-amber-400" data-testid="code-session">
                    {session.code}
                  </p>
                </div>
                <div className="text-right">
                  {!cloturee ? (
                    <>
                      <Badge variant={minutesRestantes > 5 ? "success" : "warning"} data-testid="badge-expiration">
                        Expire dans {minutesRestantes} min
                      </Badge>
                      <div className="mt-3">
                        {!confirmation ? (
                          <Button variant="outline" size="sm" onClick={() => setConfirmation(true)} data-testid="btn-cloturer">
                            <StopCircle className="h-4 w-4" /> Clôturer la session
                          </Button>
                        ) : (
                          <div className="flex items-center gap-2" data-testid="zone-confirmation">
                            <span className="text-sm font-medium text-destructive">Irréversible ?</span>
                            <Button variant="destructive" size="sm" onClick={cloturer} data-testid="btn-confirmer-cloture">Confirmer</Button>
                            <Button variant="ghost" size="sm" onClick={() => setConfirmation(false)} data-testid="btn-annuler-cloture">Annuler</Button>
                          </div>
                        )}
                      </div>
                    </>
                  ) : (
                    <Badge variant="secondary" data-testid="badge-cloturee">Session clôturée</Badge>
                  )}
                </div>
              </div>
              {messageCloture && <div className="mt-4"><Succes>{messageCloture}</Succes></div>}
            </div>
          )}
        </CardContent>
      </Card>

      {/* PRÉSENCE MANUELLE */}
      <Card data-testid="carte-presence-manuelle" className={!session || cloturee ? "opacity-60" : ""}>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserPlus className="h-5 w-5 text-amber-600" /> Ajouter une présence à la main
          </CardTitle>
          <CardDescription>Pour un étudiant qui n'aurait pas pu saisir le code (retard, oubli…).</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {!session && <Badge variant="warning">Ouvrez d'abord une session</Badge>}
          {session && cloturee && <Badge variant="warning">Session clôturée — ajout impossible (RG13)</Badge>}
          <form onSubmit={marquerPresent} className="flex max-w-md gap-2">
            <Select
              data-testid="select-etudiant-formateur"
              value={etudiantChoisi}
              onChange={(e) => setEtudiantChoisi(e.target.value)}
              required
              disabled={!session || cloturee}
            >
              <option value="" disabled>— Choisir un étudiant —</option>
              {etudiants.map((e) => (
                <option key={e.id} value={e.id}>{e.nom}</option>
              ))}
            </Select>
            <Button type="submit" disabled={!session || cloturee || !etudiantChoisi} data-testid="btn-presence-formateur">
              Marquer présent
            </Button>
          </form>
          {messagePresence && <Succes>{messagePresence}</Succes>}
          <Etat chargement={false} erreur={erreurPresence} />
        </CardContent>
      </Card>

      {/* TABLEAU */}
      <Card data-testid="carte-tableau">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <LayoutDashboard className="h-5 w-5 text-amber-600" /> Tableau de la promotion
            </CardTitle>
            <Button variant="ghost" size="sm" onClick={chargerTableau} data-testid="btn-refresh-tableau">
              <RefreshCw className="h-4 w-4" /> Actualiser
            </Button>
          </div>
          <CardDescription>Agrégat calculé côté serveur — la moyenne n'est jamais recalculée ici (F3).</CardDescription>
        </CardHeader>
        <CardContent>
          <Etat chargement={chargementTableau} erreur={erreurTableau} onRetry={chargerTableau}>
            {lignes && (
              <div className="overflow-x-auto rounded-lg border">
                <table className="w-full text-sm" data-testid="tableau">
                  <thead className="bg-muted/50 text-left text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3 font-medium">Étudiant</th>
                      <th className="px-4 py-3 font-medium text-center">Présences</th>
                      <th className="px-4 py-3 font-medium text-center">Exercices</th>
                      <th className="px-4 py-3 font-medium text-center">Moyenne</th>
                      <th className="px-4 py-3 font-medium text-center">Relectures dues</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {lignes.map((l) => (
                      <tr key={l.etudiantId} className="transition-colors hover:bg-muted/30" data-testid={`ligne-${l.nom}`}>
                        <td className="px-4 py-3 font-medium">{l.nom}</td>
                        <td className="px-4 py-3 text-center">{l.presences}</td>
                        <td className="px-4 py-3 text-center">{l.exercicesDeposes}</td>
                        <td className="px-4 py-3 text-center font-semibold">
                          {l.moyenne == null ? <span className="text-muted-foreground">—</span> : `${l.moyenne.toFixed(2)} /20`}
                        </td>
                        <td className="px-4 py-3 text-center">
                          {l.relecturesEnAttente > 0
                            ? <Badge variant="warning">⚠ {l.relecturesEnAttente}</Badge>
                            : <span className="text-muted-foreground">0</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Etat>
        </CardContent>
      </Card>
    </div>
  )
}
