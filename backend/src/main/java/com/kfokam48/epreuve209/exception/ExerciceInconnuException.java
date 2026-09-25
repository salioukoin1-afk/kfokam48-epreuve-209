package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 404 EXERCICE_INCONNU — aucun exercice pour cette (session, étudiant). */
public class ExerciceInconnuException extends ErreurApiException {
    public ExerciceInconnuException() {
        super("EXERCICE_INCONNU", "Aucun exercice déposé pour cette session.", HttpStatus.NOT_FOUND);
    }
}
