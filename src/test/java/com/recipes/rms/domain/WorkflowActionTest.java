package com.recipes.rms.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowActionTest {

    @Test
    void transitionsMatchTheDocumentedStateMachine() {
        assertThat(WorkflowAction.SUBMIT.getFrom()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(WorkflowAction.SUBMIT.getTo()).isEqualTo(RecipeStatus.PENDING_REVIEW);
        assertThat(WorkflowAction.APPROVE.getFrom()).isEqualTo(RecipeStatus.PENDING_REVIEW);
        assertThat(WorkflowAction.APPROVE.getTo()).isEqualTo(RecipeStatus.PUBLISHED);
        assertThat(WorkflowAction.REJECT.getTo()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(WorkflowAction.UNPUBLISH.getFrom()).isEqualTo(RecipeStatus.PUBLISHED);
        assertThat(WorkflowAction.UNPUBLISH.getTo()).isEqualTo(RecipeStatus.DRAFT);
    }

    @Test
    void availableActionsPerStatus() {
        assertThat(WorkflowAction.availableFrom(RecipeStatus.DRAFT)).containsExactly(WorkflowAction.SUBMIT);
        assertThat(WorkflowAction.availableFrom(RecipeStatus.PENDING_REVIEW))
                .containsExactly(WorkflowAction.APPROVE, WorkflowAction.REJECT);
        assertThat(WorkflowAction.availableFrom(RecipeStatus.PUBLISHED)).containsExactly(WorkflowAction.UNPUBLISH);
    }

    @Test
    void onlySubmitIsRestrictedToTheOwner() {
        assertThat(WorkflowAction.SUBMIT.isOwnerOnly()).isTrue();
        assertThat(WorkflowAction.SUBMIT.getRoles()).containsExactly(Role.AUTHOR);
        assertThat(WorkflowAction.APPROVE.isOwnerOnly()).isFalse();
        assertThat(WorkflowAction.APPROVE.getRoles()).containsExactly(Role.REVIEWER);
    }
}
