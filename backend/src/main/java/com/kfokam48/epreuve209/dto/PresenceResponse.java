package com.kfokam48.epreuve209.dto;

/** Correspondance exacte avec le contrat : { id, sessionId, etudiantId, source }. */
public record PresenceResponse(
        Long id,
        Long sessionId,
        Long etudiantId,
        String source
) {}
