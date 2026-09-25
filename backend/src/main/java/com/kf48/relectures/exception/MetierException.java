package com.kf48.relectures.exception;

import org.springframework.http.HttpStatus;

public class MetierException extends RuntimeException {

    private final HttpStatus statut;
    private final String code;

    public MetierException(HttpStatus statut, String code) {
        super(MessagesErreur.libelle(code));
        this.statut = statut;
        this.code = code;
    }

    public HttpStatus getStatut() { return statut; }
    public String getCode() { return code; }
}