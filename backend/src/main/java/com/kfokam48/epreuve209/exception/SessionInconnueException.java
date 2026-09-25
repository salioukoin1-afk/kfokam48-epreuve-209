package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 404 SESSION_INCONNUE — l'identifiant de session ne correspond à rien. */
public class SessionInconnueException extends ErreurApiException {
    public SessionInconnueException(Long id) {
        super("SESSION_INCONNUE", "La session " + id + " n'existe pas.", HttpStatus.NOT_FOUND);
    }
}
