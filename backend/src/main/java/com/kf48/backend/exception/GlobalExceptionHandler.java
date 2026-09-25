package com.kf48.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

/** B4 : toute erreur renvoyee par l'API suit le format { code, message } du contrat. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MetierException.class)
    public ResponseEntity<ErreurDto> metier(MetierException e) {
        return ResponseEntity.status(e.getStatut()).body(new ErreurDto(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurDto> validation(MethodArgumentNotValidException e) {
        String code = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("CHAMP_MANQUANT");
        return erreur(HttpStatus.BAD_REQUEST, code);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErreurDto> requeteInvalide(Exception e) {
        return erreur(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErreurDto> introuvable(NoResourceFoundException e) {
        return erreur(HttpStatus.NOT_FOUND, "RESSOURCE_INTROUVABLE");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErreurDto> methode(HttpRequestMethodNotSupportedException e) {
        return erreur(HttpStatus.METHOD_NOT_ALLOWED, "METHODE_NON_AUTORISEE");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurDto> inattendue(Exception e) {
        LOG.error("Erreur non prevue", e);
        return erreur(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE");
    }

    private ResponseEntity<ErreurDto> erreur(HttpStatus statut, String code) {
        return ResponseEntity.status(statut).body(new ErreurDto(code, MessagesErreur.libelle(code)));
    }
}