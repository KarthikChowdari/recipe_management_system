package com.recipes.rms.selenium;

import com.recipes.rms.selenium.pages.RecipeDetailPage;
import com.recipes.rms.selenium.support.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Journey 3: updating recipe details and status")
class UpdateRecipeWorkflowSeleniumTest extends BaseSeleniumTest {

    @Test
    @DisplayName("author edits and submits a draft; reviewer rejects, author resubmits, reviewer publishes")
    void editSubmitReviewPublish() {
        String title = unique("Lasagne");
        String updatedTitle = title + " al Forno";

        // Author creates and edits a draft
        RecipeDetailPage detail = loginAs(AUTHOR, AUTHOR_PASSWORD)
                .clickNewRecipe().fillValid(title, "Italian", "Main Course").save();
        detail = detail.edit().title(updatedTitle).servings(6).save();

        assertThat(detail.title()).isEqualTo(updatedTitle);
        assertThat(detail.servings()).isEqualTo("6");
        assertThat(detail.successMessage()).hasValue("Recipe updated.");

        // Author submits for review: status changes and editing is locked
        detail = detail.perform("SUBMIT", "Ready for review");
        assertThat(detail.status()).isEqualTo("Pending review");
        assertThat(detail.canEdit()).isFalse();
        assertThat(detail.availableActions()).isEmpty();

        // Reviewer rejects with a comment -> back to draft
        logout();
        detail = loginAs(REVIEWER, REVIEWER_PASSWORD).searchByTitle(updatedTitle).open(updatedTitle);
        assertThat(detail.availableActions()).containsExactly("APPROVE", "REJECT");
        detail = detail.perform("REJECT", "Please add oven temperature");
        assertThat(detail.status()).isEqualTo("Draft");

        // Author fixes and resubmits
        logout();
        detail = loginAs(AUTHOR, AUTHOR_PASSWORD).searchByTitle(updatedTitle).open(updatedTitle);
        detail = detail.edit().instructions("1. Layer pasta and ragu.\n2. Bake at 180C for 45 minutes.").save();
        detail = detail.perform("SUBMIT", null);
        assertThat(detail.status()).isEqualTo("Pending review");

        // Reviewer approves -> published, full audit trail visible
        logout();
        detail = loginAs(REVIEWER, REVIEWER_PASSWORD).searchByTitle(updatedTitle).open(updatedTitle);
        detail = detail.perform("APPROVE", "Looks delicious");

        assertThat(detail.status()).isEqualTo("Published");
        assertThat(detail.availableActions()).containsExactly("UNPUBLISH");
        assertThat(detail.historyActions())
                .containsExactly("Approve & publish", "Submit for review", "Reject", "Submit for review");
    }
}
