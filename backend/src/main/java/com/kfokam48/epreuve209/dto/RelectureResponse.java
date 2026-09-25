package com.kfokam48.epreuve209.dto;

/** Réponse d'un rendu (POST) ou d'une correction (PATCH) — aucune donnée du relecteur (RG7). */
public record RelectureResponse(
        Long id,
        Long exerciceId,
        int note,
        String commentaire
) {}
