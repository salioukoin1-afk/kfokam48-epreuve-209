package com.kfokam48.epreuve209.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoterRelectureRequest(

        @NotNull(message = "La note est requise")
        Integer note,

        @NotBlank(message = "Le commentaire est requis")
        String commentaire
) {}
