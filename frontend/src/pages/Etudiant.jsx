import { useCallback, useEffect, useState } from "react"
import { UserRound, KeyRound, Link2, IdCard, Info } from "lucide-react"
import Etat, { Succes } from "@/components/Etat.jsx"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label, Select } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { presencesApi, exercicesApi, etudiantsApi } from "@/api/modules.js"

const PROMOTION_DEMO = 1

export default function EcranEtudiant() {
  // Q1 : l'étudiant se choisit dans une liste — AUCUNE saisie de code avant ce choix.
  const [etudiants, setEtudiants] = useState(null)
  const [chargementListe, setChargementListe] = useState(true)
  const [erreurListe, setErreurListe] = useState(null)
  const [etudiantId, setEtudiantId] = useState("")
  const [etudiantNom, setEtudiantNom] = useState("")

  const [code, setCode] = useState("")
  const [messagePresence, setMessagePresence] = useState("")
  const [erreurPresence, setErreurPresence] = useState(null)

  // sessionId déduit de la réponse POST /api/presences (champ sessionId)
  const [sessionId, setSessionId] = useState(null)

  const [lien, setLien] = useState("")
  const [messageDepot, setMessageDepot] = useState("")
  const [erreurDepot, setErreurDepot] = useState(null)

  const [exercice, setExercice] = useState(null)
  const [chargementExercice, setChargementExercice] = useState(false)
  const [erreurExercice, setErreurExercice] = useState(null)

  const chargerListe = useCallback(() => {
    setChargementListe(true)
    setErreurListe(null)
    etudiantsApi.liste(PROMOTION_DEMO)
      .then(setEtudiants)
      .catch(setErreurListe)
      .finally(() => setChargementListe(false))
  }, [])

  useEffect(chargerListe, [chargerListe])

  const identifie = Boolean(etudiantId)

  // Charger l'exercice uniquement quand on connaît etudiantId ET sessionId
  const chargerMonExercice = useCallback(() => {
    if (!identifie || !sessionId) return
    setChargementExercice(true)
    setErreurExercice(null)
    exercicesApi.monExercice(sessionId, Number(etudiantId))
      .then((ex) => { setExercice(ex); setErreurExercice(null) })
      .catch((err) => {
        // EXERCICE_INCONNU = état nominal (rien encore déposé) : pas une erreur à afficher.
        if (err.code === "EXERCICE_INCONNU") { setExercice(null); setErreurExercice(null) }
        else setErreurExercice(err)
      })
      .finally(() => setChargementExercice(false))
  }, [etudiantId, sessionId, identifie])

  useEffect(chargerMonExercice, [chargerMonExercice])

  const choisirNom = (id) => {
    setEtudiantId(id)
    setEtudiantNom(etudiants?.find((e) => String(e.id) === id)?.nom ?? "")
    // Réinitialiser l'état lié à la session précédente
    setSessionId(null)
    setExercice(null)
    setMessagePresence("")
    setErreurPresence(null)
    setMessageDepot("")
    setErreurDepot(null)
  }

  const marquer = async (e) => {
    e.preventDefault()
    setErreurPresence(null)
    setMessagePresence("")
    try {
      const presence = await presencesApi.marquer(code.trim(), Number(etudiantId))
      // Récupérer le sessionId depuis la réponse (RG2 — clé du flux)
      setSessionId(presence.sessionId)
      setMessagePresence(`Présence enregistrée pour ${etudiantNom} ✓`)
      setCode("")
    } catch (err) {
      setErreurPresence(err)
    }
  }

  const deposer = async (e) => {
    e.preventDefault()
    setErreurDepot(null)
    setMessageDepot("")
    try {
      const ex = await exercicesApi.deposer(sessionId, Number(etudiantId), lien.trim())
      setMessageDepot(`Exercice déposé — statut : ${ex.statut}`)
      setLien("")
      chargerMonExercice()
    } catch (err) {
      setErreurDepot(err)
    }
  }

  const depotDisponible = identifie && Boolean(sessionId)

  return (
    <div className="space-y-6 animate-fade-in">

      {/* ÉTAPE 1 — identité */}
      <Card data-testid="carte-identite">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserRound className="h-5 w-5 text-emerald-600" /> Étape 1 — Qui êtes-vous ?
          </CardTitle>
          <CardDescription>
            Sélectionnez votre nom dans la liste de la promotion (Q1 — aucun mot de passe).
            Cette étape est obligatoire avant toute action.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Etat chargement={chargementListe} erreur={erreurListe} onRetry={chargerListe}>
            {etudiants && (
              <div className="max-w-sm space-y-2">
                <Label htmlFor="select-etudiant">Votre nom</Label>
                <Select
                  id="select-etudiant"
                  data-testid="select-etudiant"
                  value={etudiantId}
                  onChange={(e) => choisirNom(e.target.value)}
                >
                  <option value="" disabled>— Choisissez votre nom —</option>
                  {etudiants.map((e) => (
                    <option key={e.id} value={e.id}>{e.nom}</option>
                  ))}
                </Select>
                {identifie && (
                  <p className="text-sm text-muted-foreground">
                    Bienvenue, <span className="font-semibold text-foreground">{etudiantNom}</span> —
                    saisissez le code de présence à l'étape 2.
                  </p>
                )}
              </div>
            )}
          </Etat>
        </CardContent>
      </Card>

      {/* ÉTAPE 2 — présence */}
      <Card className={identifie ? "" : "opacity-60"} data-testid="carte-presence">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <KeyRound className="h-5 w-5 text-emerald-600" /> Étape 2 — Marquer ma présence
          </CardTitle>
          <CardDescription>
            Saisissez le code communiqué par le formateur. Le sessionId est automatiquement
            récupéré depuis la réponse de présence.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {!identifie && (
            <Badge variant="warning">Sélectionnez d'abord votre nom à l'étape 1</Badge>
          )}
          <form onSubmit={marquer} className="flex max-w-md gap-2">
            <Input
              data-testid="input-code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="Code de la session (ex. AB12CD)"
              required
              disabled={!identifie}
              className="uppercase tracking-widest font-mono"
            />
            <Button type="submit" disabled={!identifie} data-testid="btn-presence">
              Valider
            </Button>
          </form>
          {messagePresence && <Succes>{messagePresence}</Succes>}
          {sessionId && (
            <p className="flex items-center gap-1 text-xs text-muted-foreground" data-testid="session-id-info">
              <Info className="h-3.5 w-3.5" />
              Session #{sessionId} liée — dépôt d'exercice débloqué.
            </p>
          )}
          <Etat chargement={false} erreur={erreurPresence} />
        </CardContent>
      </Card>

      {/* ÉTAPE 3 — dépôt */}
      <Card className={depotDisponible ? "" : "opacity-60"} data-testid="carte-depot">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Link2 className="h-5 w-5 text-emerald-600" /> Étape 3 — Déposer mon exercice
          </CardTitle>
          <CardDescription>
            Lien de votre dépôt (GitHub, Drive…). Un nouveau lien remplace l'ancien tant que la note n'est pas rendue.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {!identifie && (
            <Badge variant="warning">Sélectionnez d'abord votre nom à l'étape 1</Badge>
          )}
          {identifie && !sessionId && (
            <Badge variant="warning">Marquez votre présence (étape 2) pour débloquer le dépôt</Badge>
          )}
          <form onSubmit={deposer} className="flex max-w-xl gap-2">
            <Input
              data-testid="input-lien"
              type="url"
              value={lien}
              onChange={(e) => setLien(e.target.value)}
              placeholder="https://…"
              required
              disabled={!depotDisponible}
            />
            <Button type="submit" disabled={!depotDisponible} data-testid="btn-depot">
              Déposer
            </Button>
          </form>
          {messageDepot && <Succes>{messageDepot}</Succes>}
          <Etat chargement={false} erreur={erreurDepot} />
        </CardContent>
      </Card>

      {/* ÉTAPE 4 — résultat */}
      <Card data-testid="carte-note">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <IdCard className="h-5 w-5 text-emerald-600" /> Ma note
          </CardTitle>
          <CardDescription>
            Votre évaluation — l'identité du relecteur reste anonyme (RG7 / Q8).
          </CardDescription>
        </CardHeader>
        <CardContent>
          {!identifie ? (
            <Badge variant="warning">Sélectionnez d'abord votre nom à l'étape 1</Badge>
          ) : !sessionId ? (
            <p className="text-sm text-muted-foreground">
              Marquez votre présence (étape 2) pour consulter votre exercice.
            </p>
          ) : (
            <Etat chargement={chargementExercice} erreur={erreurExercice} onRetry={chargerMonExercice}>
              {exercice ? (
                <div className="flex items-center gap-4">
                  <Badge variant={exercice.statut === "RELU" ? "success" : "secondary"} data-testid="statut-exercice">
                    {exercice.statut}
                  </Badge>
                  {exercice.note != null ? (
                    <div data-testid="zone-note">
                      <span className="text-3xl font-bold">
                        {exercice.note}
                        <span className="text-base font-normal text-muted-foreground">/20</span>
                      </span>
                      <p className="mt-1 text-sm text-muted-foreground" data-testid="commentaire-note">
                        « {exercice.commentaire} »
                      </p>
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground" data-testid="attente-note">
                      En attente de relecture…
                    </p>
                  )}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground" data-testid="aucun-exercice">
                  Aucun exercice déposé pour cette session.
                </p>
              )}
            </Etat>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
