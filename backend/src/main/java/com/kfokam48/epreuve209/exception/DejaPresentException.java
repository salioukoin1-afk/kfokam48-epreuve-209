package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 409 DEJA_PRESENT — RG15 : une seule présence par (session, étudiant). */
public class DejaPresentException extends ErreurApiException {
    public DejaPresentException() {
        super("DEJA_PRESENT", "Cet étudiant est déjà marqué présent pour cette session.", HttpStatus.CONFLICT);
    }
}
