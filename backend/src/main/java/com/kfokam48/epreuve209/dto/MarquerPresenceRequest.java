package com.kfokam48.epreuve209.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarquerPresenceRequest(

        @NotBlank(message = "Le code de présence est requis")
        String code,

        @NotNull(message = "L'étudiant est requis")
        Long etudiantId
) {}
