package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 400 BLOCAGE_CLOTURE — la session est déjà clôturée : clotureeAt n'est jamais écrasé (US-08). */
public class BlocageClotureException extends ErreurApiException {
    public BlocageClotureException() {
        super("BLOCAGE_CLOTURE", "Cette session est déjà clôturée.", HttpStatus.BAD_REQUEST);
    }
}
