package com.recipes.rms.service;

public class RecipeNotFoundException extends RuntimeException {

    public RecipeNotFoundException(Long id) {
        super("Recipe " + id + " not found");
    }
}
