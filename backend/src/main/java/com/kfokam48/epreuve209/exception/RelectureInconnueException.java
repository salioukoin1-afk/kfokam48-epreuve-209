package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 404 RELECTURE_INCONNUE — l'identifiant de relecture ne correspond à rien. */
public class RelectureInconnueException extends ErreurApiException {
    public RelectureInconnueException(Long id) {
        super("RELECTURE_INCONNUE", "La relecture " + id + " n'existe pas.", HttpStatus.NOT_FOUND);
    }
}
