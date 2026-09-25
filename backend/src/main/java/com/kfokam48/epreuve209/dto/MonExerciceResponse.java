package com.kfokam48.epreuve209.dto;

/**
 * Vue de l'auteur sur son exercice (EF9). RG7 : ce DTO ne contient AUCUN champ
 * relecteurId ni relecteurNom — l'anonymat est garanti par l'API elle-même, pas
 * seulement par l'écran (test dédié dans MonExerciceControllerTest).
 */
public record MonExerciceResponse(
        Long id,
        String statut,
        String lien,
        Integer note,
        String commentaire
) {}
