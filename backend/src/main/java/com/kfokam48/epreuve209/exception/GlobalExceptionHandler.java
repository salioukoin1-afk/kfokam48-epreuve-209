package com.kfokam48.epreuve209.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * B4 : une seule porte de sortie pour toutes les erreurs.
 * Format d'erreur imposé, sans exception : { "code": "...", "message": "..." }.
 * Le champ "details" ne sert qu'aux erreurs de validation de champs (400), où le
 * client a besoin de savoir quel champ est en cause ; toutes les autres erreurs
 * respectent strictement les deux champs du contrat.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record CorpsErreur(String code, String message, Map<String, String> details) {
        public CorpsErreur(String code, String message) { this(code, message, null); }
    }

    @ExceptionHandler(ErreurApiException.class)
    public ResponseEntity<CorpsErreur> metier(ErreurApiException ex) {
        // Le statut est porté par l'exception : le contrat impose des statuts
        // différents pour un même code selon l'opération (PROMOTION_INCONNUE 400/404).
        return ResponseEntity.status(ex.status())
                .body(new CorpsErreur(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CorpsErreur> validation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> details.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new CorpsErreur("CHAMP_MANQUANT", "Requête invalide.", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CorpsErreur> inattendue(Exception ex) {
        // B4 : jamais de stack trace vers le client — mais toujours tracée côté serveur,
        // sinon une erreur 500 est indébuggable (leçon du test en conteneur réel).
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
        log.error("Erreur interne non gérée", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CorpsErreur("ERREUR_INTERNE", "Une erreur interne est survenue."));
    }
}
