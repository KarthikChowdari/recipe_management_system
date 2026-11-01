package com.recipes.rms.web.api;

import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.domain.StatusHistory;
import com.recipes.rms.domain.WorkflowAction;

import java.time.LocalDateTime;

public record StatusHistoryResponse(WorkflowAction action, RecipeStatus fromStatus, RecipeStatus toStatus,
                                    String changedBy, String comment, LocalDateTime changedAt) {

    public static StatusHistoryResponse from(StatusHistory h) {
        return new StatusHistoryResponse(h.getAction(), h.getFromStatus(), h.getToStatus(), h.getChangedBy(),
                h.getComment(), h.getChangedAt());
    }
}
