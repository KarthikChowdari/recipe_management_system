package com.recipes.rms.service;

/** The actor is authenticated but lacks the role or ownership needed for the operation (HTTP 403). */
public class OperationNotPermittedException extends RuntimeException {

    public OperationNotPermittedException(String message) {
        super(message);
    }
}
