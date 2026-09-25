package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/**
 * Base de toutes les exceptions métier : porte le code d'erreur stable du contrat
 * ET son statut HTTP, car le contrat impose des statuts différents selon l'opération
 * pour un même code (PROMOTION_INCONNUE : 400 sur POST /api/sessions, 404 sur GET /api/tableau).
 */
public class ErreurApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ErreurApiException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public ErreurApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
}
