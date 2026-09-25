package com.kfokam48.epreuve209.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OuvrirSessionRequest(

        @NotBlank(message = "Le titre de la session est requis")
        @Size(min = 3, max = 100, message = "Le titre doit compter entre 3 et 100 caractères")
        String titre,

        @NotNull(message = "La promotion est requise")
        Long promotionId
) {}
