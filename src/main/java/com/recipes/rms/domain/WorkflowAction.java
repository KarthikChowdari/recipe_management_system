package com.recipes.rms.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The recipe review state machine. Every status change goes through one of these actions;
 * each action declares its source and target status and which roles may perform it.
 */
public enum WorkflowAction {

    SUBMIT("Submit for review", RecipeStatus.DRAFT, RecipeStatus.PENDING_REVIEW, true, Role.AUTHOR),
    APPROVE("Approve & publish", RecipeStatus.PENDING_REVIEW, RecipeStatus.PUBLISHED, false, Role.REVIEWER),
    REJECT("Reject", RecipeStatus.PENDING_REVIEW, RecipeStatus.DRAFT, false, Role.REVIEWER),
    UNPUBLISH("Unpublish", RecipeStatus.PUBLISHED, RecipeStatus.DRAFT, false, Role.REVIEWER);

    private final String label;
    private final RecipeStatus from;
    private final RecipeStatus to;
    private final boolean ownerOnly;
    private final Set<Role> roles;

    WorkflowAction(String label, RecipeStatus from, RecipeStatus to, boolean ownerOnly, Role... roles) {
        this.label = label;
        this.from = from;
        this.to = to;
        this.ownerOnly = ownerOnly;
        this.roles = EnumSet.copyOf(Arrays.asList(roles));
    }

    public static List<WorkflowAction> availableFrom(RecipeStatus status) {
        return Arrays.stream(values()).filter(a -> a.from == status).toList();
    }

    public String getLabel() {
        return label;
    }

    public RecipeStatus getFrom() {
        return from;
    }

    public RecipeStatus getTo() {
        return to;
    }

    /** When true, a non-admin may only perform the action on recipes they created. */
    public boolean isOwnerOnly() {
        return ownerOnly;
    }

    /** Roles allowed to perform the action, in addition to ADMIN which may perform every action. */
    public Set<Role> getRoles() {
        return EnumSet.copyOf(roles);
    }
}
