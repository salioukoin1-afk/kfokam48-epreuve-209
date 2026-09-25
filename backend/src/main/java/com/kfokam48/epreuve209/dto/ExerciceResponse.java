package com.kfokam48.epreuve209.dto;

/** Correspondance exacte avec le contrat : { id, statut }. */
public record ExerciceResponse(
        Long id,
        String statut
) {}
