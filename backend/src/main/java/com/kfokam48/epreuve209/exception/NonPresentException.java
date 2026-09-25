package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 400 NON_PRESENT — décision section 7 du CDC : le dépôt exige une présence enregistrée. */
public class NonPresentException extends ErreurApiException {
    public NonPresentException() {
        super("NON_PRESENT", "Vous devez être présent à la session pour déposer votre exercice.", HttpStatus.BAD_REQUEST);
    }
}
