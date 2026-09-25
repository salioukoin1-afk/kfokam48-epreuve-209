package com.kfokam48.epreuve209.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeposerExerciceRequest(

        @NotNull(message = "La session est requise")
        Long sessionId,

        @NotNull(message = "L'étudiant est requis")
        Long etudiantId,

        @NotBlank(message = "Le lien de l'exercice est requis")
        String lien
) {}
