package com.recipes.rms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Audit record written for every workflow transition. */
@Entity
@Table(name = "recipe_status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private RecipeStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private RecipeStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowAction action;

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(length = 500)
    private String comment;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected StatusHistory() {
    }

    public StatusHistory(Recipe recipe, RecipeStatus fromStatus, WorkflowAction action, String changedBy,
                         String comment) {
        this.recipe = recipe;
        this.fromStatus = fromStatus;
        this.toStatus = action.getTo();
        this.action = action;
        this.changedBy = changedBy;
        this.comment = comment;
    }

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public RecipeStatus getFromStatus() {
        return fromStatus;
    }

    public RecipeStatus getToStatus() {
        return toStatus;
    }

    public WorkflowAction getAction() {
        return action;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
