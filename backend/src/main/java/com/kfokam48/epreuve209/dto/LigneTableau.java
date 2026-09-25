package com.kfokam48.epreuve209.dto;

/**
 * Correspondance exacte avec le contrat GET /api/tableau :
 * { etudiantId, nom, presences, exercicesDeposes, moyenne, relecturesEnAttente }.
 * F3 : la moyenne est calculée CÔTÉ SERVEUR — le frontend ne la recalcule jamais.
 */
public record LigneTableau(
        Long etudiantId,
        String nom,
        int presences,
        int exercicesDeposes,
        Double moyenne,
        int relecturesEnAttente
) {}
