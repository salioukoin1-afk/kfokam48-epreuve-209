package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;

public class PromotionInconnueException extends ErreurApiException {
    public PromotionInconnueException(Long promotionId, HttpStatus status) {
        super("PROMOTION_INCONNUE", "La promotion " + promotionId + " n'existe pas.", status);
    }
}
