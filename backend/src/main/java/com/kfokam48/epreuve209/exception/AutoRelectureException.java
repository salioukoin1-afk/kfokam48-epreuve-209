package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 403 AUTO_RELECTURE — imposé par le contrat : RG4, un étudiant ne relire jamais son propre exercice. */
public class AutoRelectureException extends ErreurApiException {
    public AutoRelectureException() {
        super("AUTO_RELECTURE", "Vous ne pouvez pas relire votre propre exercice.", HttpStatus.FORBIDDEN);
    }
}
