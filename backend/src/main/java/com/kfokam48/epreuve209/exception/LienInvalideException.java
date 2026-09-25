package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 400 LIEN_INVALIDE — imposé par le contrat. */
public class LienInvalideException extends ErreurApiException {
    public LienInvalideException() {
        super("LIEN_INVALIDE", "Le lien de l'exercice n'est pas une URL valide.", HttpStatus.BAD_REQUEST);
    }
}
