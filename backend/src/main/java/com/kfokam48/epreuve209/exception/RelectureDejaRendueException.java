package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 409 RELECTURE_DEJA_RENDUE — imposé par le contrat : le POST n'est qu'une création. */
public class RelectureDejaRendueException extends ErreurApiException {
    public RelectureDejaRendueException() {
        super("RELECTURE_DEJA_RENDUE", "Cette relecture a déjà été rendue.", HttpStatus.CONFLICT);
    }
}
