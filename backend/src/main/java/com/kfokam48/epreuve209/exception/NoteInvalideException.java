package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 400 NOTE_INVALIDE — imposé par le contrat : entier, bornes 0 et 20 incluses (RG8). */
public class NoteInvalideException extends ErreurApiException {
    public NoteInvalideException() {
        super("NOTE_INVALIDE", "La note doit être un entier compris entre 0 et 20.", HttpStatus.BAD_REQUEST);
    }
}
