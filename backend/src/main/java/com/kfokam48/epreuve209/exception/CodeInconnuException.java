package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 400 CODE_INCONNU — alimente le compteur RG3 (le code saisi ne correspond à aucune session active). */
public class CodeInconnuException extends ErreurApiException {
    public CodeInconnuException() {
        super("CODE_INCONNU", "Le code de présence ne correspond à aucune session en cours.", HttpStatus.BAD_REQUEST);
    }
}
