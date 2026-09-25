import { useCallback, useEffect, useState } from "react"
import { ClipboardList, CheckCircle2, UserRound, PencilLine } from "lucide-react"
import Etat, { Succes } from "@/components/Etat.jsx"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label, Select } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { relecturesApi, etudiantsApi } from "@/api/modules.js"

const PROMOTION_DEMO = 1

export default function EcranRelecteur() {
  // Étape 1 — le relecteur se choisit dans la liste (même mécanique Q1 que l'étudiant).
  const [etudiants, setEtudiants] = useState([])
  const [relecteurId, setRelecteurId] = useState("")

  const [relectures, setRelectures] = useState(null)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState(null)

  // États par relectureId
  const [notes, setNotes] = useState({})
  const [commentaires, setCommentaires] = useState({})
  const [messages, setMessages] = useState({})
  const [erreurs, setErreurs] = useState({})
  // Track des relectures déjà rendues (pour proposer la correction PATCH)
  const [rendues, setRendues] = useState({})

  useEffect(() => {
    etudiantsApi.liste(PROMOTION_DEMO).then(setEtudiants).catch(() => setEtudiants([]))
  }, [])

  const charger = useCallback(() => {
    if (!relecteurId) return
    setChargement(true)
    setErreur(null)
    relecturesApi.enAttente(Number(relecteurId))
      .then((liste) => { setRelectures(liste); setErreur(null) })
      .catch(setErreur)
      .finally(() => setChargement(false))
  }, [relecteurId])

  useEffect(() => { charger() }, [charger])

  const rendre = async (r, e) => {
    e.preventDefault()
    const note = notes[r.relectureId]
    const commentaire = commentaires[r.relectureId] ?? ""
    setErreurs((p) => ({ ...p, [r.relectureId]: null }))
    setMessages((p) => ({ ...p, [r.relectureId]: "" }))
    try {
      await relecturesApi.rendre(r.relectureId, Number(relecteurId), Number(note), commentaire)
      setMessages((p) => ({ ...p, [r.relectureId]: `Note ${note}/20 enregistrée ✓` }))
      setRendues((p) => ({ ...p, [r.relectureId]: true }))
      charger()
    } catch (err) {
      setErreurs((p) => ({ ...p, [r.relectureId]: err }))
    }
  }

  const corriger = async (r, e) => {
    e.preventDefault()
    const note = notes[r.relectureId]
    const commentaire = commentaires[r.relectureId] ?? ""
    setErreurs((p) => ({ ...p, [r.relectureId]: null }))
    setMessages((p) => ({ ...p, [r.relectureId]: "" }))
    try {
      await relecturesApi.corriger(r.relectureId, Number(relecteurId), Number(note), commentaire)
      setMessages((p) => ({ ...p, [r.relectureId]: `Correction ${note}/20 enregistrée ✓` }))
    } catch (err) {
      setErreurs((p) => ({ ...p, [r.relectureId]: err }))
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">

      {/* Identité */}
      <Card data-testid="carte-identite-relecteur">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserRound className="h-5 w-5 text-violet-600" /> Qui relit ?
          </CardTitle>
          <CardDescription>
            Sélectionnez votre nom : seuls les exercices qui vous sont assignés s'affichent (RG10).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="max-w-sm space-y-2">
            <Label htmlFor="select-relecteur">Votre nom</Label>
            <Select
              id="select-relecteur"
              data-testid="select-relecteur"
              value={relecteurId}
              onChange={(e) => { setRelecteurId(e.target.value); setRelectures(null); setRendues({}) }}
            >
              <option value="" disabled>— Choisissez votre nom —</option>
              {etudiants.map((e) => (
                <option key={e.id} value={e.id}>{e.nom}</option>
              ))}
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Liste des exercices à relire */}
      {relecteurId && (
        <Card data-testid="carte-a-relire">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <ClipboardList className="h-5 w-5 text-violet-600" /> Exercices à relire
            </CardTitle>
            <CardDescription>
              Note modifiable jusqu'à la clôture de la session (RG9) — votre identité reste anonyme pour l'auteur (RG7).
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Etat chargement={chargement} erreur={erreur} onRetry={charger}>
              {relectures && relectures.length > 0 ? (
                <ul className="divide-y divide-border" data-testid="liste-relectures">
                  {relectures.map((r) => {
                    const dejaRendue = rendues[r.relectureId]
                    return (
                      <li
                        key={r.relectureId}
                        className="flex flex-col gap-4 py-5"
                        data-testid={`ligne-relecture-${r.relectureId}`}
                      >
                        {/* En-tête de l'exercice */}
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div className="min-w-0">
                            <p className="font-semibold text-foreground">{r.auteurNom}</p>
                            <a
                              href={r.lien}
                              target="_blank"
                              rel="noreferrer"
                              className="break-all text-sm text-violet-600 dark:text-violet-400 underline underline-offset-2 hover:no-underline"
                              data-testid={`lien-exercice-${r.relectureId}`}
                            >
                              {r.lien}
                            </a>
                          </div>
                          <Badge variant="secondary">{r.sessionTitre}</Badge>
                        </div>

                        {/* Formulaire de notation / correction */}
                        <form
                          onSubmit={dejaRendue ? corriger.bind(null, r) : rendre.bind(null, r)}
                          className="grid max-w-lg gap-3 sm:grid-cols-[6rem_1fr_auto] sm:items-end"
                        >
                          {/* Note */}
                          <div className="space-y-1">
                            <Label htmlFor={`note-${r.relectureId}`}>Note /20</Label>
                            <Input
                              id={`note-${r.relectureId}`}
                              data-testid={`input-note-${r.relectureId}`}
                              type="number"
                              min={0}
                              max={20}
                              value={notes[r.relectureId] ?? ""}
                              onChange={(e) => setNotes((p) => ({ ...p, [r.relectureId]: e.target.value }))}
                              placeholder="0–20"
                              required
                            />
                          </div>

                          {/* Commentaire */}
                          <div className="space-y-1">
                            <Label htmlFor={`commentaire-${r.relectureId}`}>Commentaire</Label>
                            <Input
                              id={`commentaire-${r.relectureId}`}
                              data-testid={`input-commentaire-${r.relectureId}`}
                              type="text"
                              value={commentaires[r.relectureId] ?? ""}
                              onChange={(e) => setCommentaires((p) => ({ ...p, [r.relectureId]: e.target.value }))}
                              placeholder="Votre retour sur l'exercice…"
                              required
                            />
                          </div>

                          {/* Bouton */}
                          <Button
                            type="submit"
                            variant={dejaRendue ? "outline" : "default"}
                            data-testid={dejaRendue ? `btn-corriger-${r.relectureId}` : `btn-rendre-${r.relectureId}`}
                          >
                            {dejaRendue
                              ? <><PencilLine className="h-4 w-4" /> Corriger</>
                              : <><CheckCircle2 className="h-4 w-4" /> Rendre</>}
                          </Button>
                        </form>

                        {dejaRendue && (
                          <p className="text-xs text-muted-foreground">
                            Note déjà rendue — vous pouvez la corriger jusqu'à la clôture de la session (RG9).
                          </p>
                        )}

                        {messages[r.relectureId] && <Succes>{messages[r.relectureId]}</Succes>}
                        <Etat chargement={false} erreur={erreurs[r.relectureId]} />
                      </li>
                    )
                  })}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground" data-testid="aucune-relecture">
                  Aucun exercice en attente de votre relecture. 👏
                </p>
              )}
            </Etat>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
