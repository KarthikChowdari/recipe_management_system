package com.recipes.rms.service;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.domain.Role;
import com.recipes.rms.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    private static final Actor AUTHOR = Actor.of("author", Role.AUTHOR);
    private static final Actor OTHER_AUTHOR = Actor.of("someone-else", Role.AUTHOR);
    private static final Actor REVIEWER = Actor.of("reviewer", Role.REVIEWER);
    private static final Actor ADMIN = Actor.of("admin", Role.ADMIN);

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe draft;

    @BeforeEach
    void setUp() {
        draft = new Recipe();
        draft.setId(1L);
        draft.setTitle("Tomato Soup");
        draft.setCuisine("French");
        draft.setCategory("Soup");
        draft.setIngredients("tomatoes");
        draft.setInstructions("cook");
        draft.setCreatedBy("author");
        draft.setStatus(RecipeStatus.DRAFT);
    }

    static RecipeInput input(String title) {
        RecipeInput in = new RecipeInput();
        in.setTitle(title);
        in.setCuisine(" Italian ");
        in.setCategory("Main Course");
        in.setIngredients("pasta\nsauce");
        in.setInstructions("boil, mix");
        in.setServings(2);
        return in;
    }

    @Test
    void createStartsAsDraftOwnedByActorAndTrimsInput() {
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Recipe created = recipeService.create(input("Pasta"), AUTHOR);

        assertThat(created.getStatus()).isEqualTo(RecipeStatus.DRAFT);
        assertThat(created.getCreatedBy()).isEqualTo("author");
        assertThat(created.getCuisine()).isEqualTo("Italian");
    }

    @Test
    void reviewerCannotCreate() {
        assertThatThrownBy(() -> recipeService.create(input("Pasta"), REVIEWER))
                .isInstanceOf(OperationNotPermittedException.class);
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void getUnknownRecipeThrowsNotFound() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.get(99L)).isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void ownerCanUpdateOwnDraft() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Recipe updated = recipeService.update(1L, input("Better Tomato Soup"), AUTHOR);

        assertThat(updated.getTitle()).isEqualTo("Better Tomato Soup");
    }

    @Test
    void authorCannotUpdateSomeoneElsesRecipe() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> recipeService.update(1L, input("Hijack"), OTHER_AUTHOR))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void authorCannotUpdateOwnRecipeOnceSubmitted() {
        draft.setStatus(RecipeStatus.PENDING_REVIEW);
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> recipeService.update(1L, input("Late change"), AUTHOR))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void adminCanUpdatePublishedRecipe() {
        draft.setStatus(RecipeStatus.PUBLISHED);
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(recipeService.update(1L, input("Fixed typo"), ADMIN).getTitle()).isEqualTo("Fixed typo");
    }

    @Test
    void staleVersionIsRejected() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(draft)); // stored version is null
        RecipeInput stale = input("Stale");
        stale.setVersion(5L);

        assertThatThrownBy(() -> recipeService.update(1L, stale, AUTHOR)).isInstanceOf(ConflictException.class);
    }

    @Test
    void onlyAdminCanDelete() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> recipeService.delete(1L, AUTHOR)).isInstanceOf(OperationNotPermittedException.class);

        recipeService.delete(1L, ADMIN);
        ArgumentCaptor<Recipe> deleted = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).delete(deleted.capture());
        assertThat(deleted.getValue().getId()).isEqualTo(1L);
    }
}
