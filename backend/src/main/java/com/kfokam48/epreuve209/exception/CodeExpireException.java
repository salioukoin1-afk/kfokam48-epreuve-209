package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 410 CODE_EXPIRE — RG1 : la session a été ouverte depuis plus que la durée configurée. */
public class CodeExpireException extends ErreurApiException {
    public CodeExpireException() {
        super("CODE_EXPIRE", "Le code de présence a expiré.", HttpStatus.GONE);
    }
}
