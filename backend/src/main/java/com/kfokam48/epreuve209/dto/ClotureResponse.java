package com.kfokam48.epreuve209.dto;

import java.time.LocalDateTime;

/** Correspondance avec le contrat (extension v1.1) : { id, clotureeAt }. */
public record ClotureResponse(
        Long id,
        LocalDateTime clotureeAt
) {}
