package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 409 EXERCICE_DEJA_DEPOSE — imposé par le contrat : la relecture est rendue (RELU), le lien est verrouillé. */
public class ExerciceDejaDeposeException extends ErreurApiException {
    public ExerciceDejaDeposeException() {
        super("EXERCICE_DEJA_DEPOSE", "La relecture a déjà été rendue : le lien ne peut plus être remplacé.", HttpStatus.CONFLICT);
    }
}
