package com.kfokam48.epreuve209.dto;

/** Correspondance avec le contrat (extension v1.1) : liste des exercices à relire d'un relecteur. */
public record RelecturesEnAttenteResponse(
        Long exerciceId,
        Long relectureId,
        Long sessionId,
        String sessionTitre,
        String lien,
        String auteurNom
) {}
