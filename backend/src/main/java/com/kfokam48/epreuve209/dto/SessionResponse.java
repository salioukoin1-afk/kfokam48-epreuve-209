package com.kfokam48.epreuve209.dto;

import java.time.LocalDateTime;

/** Correspondance exacte avec le contrat : { id, code, ouvertureAt, expirationAt }. */
public record SessionResponse(
        Long id,
        String code,
        LocalDateTime ouvertureAt,
        LocalDateTime expirationAt
) {}
