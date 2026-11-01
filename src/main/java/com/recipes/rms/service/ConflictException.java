package com.recipes.rms.service;

/** The request conflicts with the current state of the recipe (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
