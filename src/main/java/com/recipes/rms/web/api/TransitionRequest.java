package com.recipes.rms.web.api;

import com.recipes.rms.domain.WorkflowAction;
import com.recipes.rms.service.RecipeService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionRequest(
        @NotNull(message = "action is required (SUBMIT, APPROVE, REJECT, UNPUBLISH)") WorkflowAction action,
        @Size(max = RecipeService.MAX_COMMENT_LENGTH, message = "comment must be at most 500 characters") String comment) {
}
