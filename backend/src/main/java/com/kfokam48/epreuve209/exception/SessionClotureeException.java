package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 409 SESSION_CLOTUREE — RG2/RG9/RG11 : plus aucune écriture sur une session clôturée. */
public class SessionClotureeException extends ErreurApiException {
    public SessionClotureeException() {
        super("SESSION_CLOTUREE", "La session est clôturée : l'action n'est plus possible.", HttpStatus.CONFLICT);
    }
}
