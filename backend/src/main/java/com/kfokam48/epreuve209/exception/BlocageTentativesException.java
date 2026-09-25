package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

/** 429 BLOCAGE_TENTATIVES — RG3 : 5 échecs consécutifs ⇒ blocage 2 minutes (décision section 7). */
public class BlocageTentativesException extends ErreurApiException {

    private final long secondesRestantes;

    public BlocageTentativesException(long secondesRestantes) {
        super("BLOCAGE_TENTATIVES",
                "Trop de tentatives échouées : réessayez dans " + Math.max(1, secondesRestantes) + " s.",
                HttpStatus.TOO_MANY_REQUESTS);
        this.secondesRestantes = secondesRestantes;
    }

    public long getSecondesRestantes() { return secondesRestantes; }
}
