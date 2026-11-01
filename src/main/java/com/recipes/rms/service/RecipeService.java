package com.recipes.rms.service;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.domain.Role;
import com.recipes.rms.domain.StatusHistory;
import com.recipes.rms.domain.WorkflowAction;
import com.recipes.rms.repository.RecipeRepository;
import com.recipes.rms.repository.RecipeSearchCriteria;
import com.recipes.rms.repository.RecipeSpecifications;
import com.recipes.rms.repository.StatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
    public static final int MAX_COMMENT_LENGTH = 500;

    private final RecipeRepository recipeRepository;
    private final StatusHistoryRepository historyRepository;

    public RecipeService(RecipeRepository recipeRepository, StatusHistoryRepository historyRepository) {
        this.recipeRepository = recipeRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public Page<Recipe> search(RecipeSearchCriteria criteria, Pageable pageable) {
        RecipeSearchCriteria effective = criteria == null ? RecipeSearchCriteria.empty() : criteria;
        return recipeRepository.findAll(RecipeSpecifications.matching(effective), pageable);
    }

    @Transactional(readOnly = true)
    public List<String> cuisines() {
        return recipeRepository.findDistinctCuisines();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return recipeRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<StatusHistory> history(Long id) {
        get(id);
        return historyRepository.findByRecipeIdOrderByChangedAtDescIdDesc(id);
    }

    @Transactional(readOnly = true)
    public Recipe get(Long id) {
        return recipeRepository.findById(id).orElseThrow(() -> new RecipeNotFoundException(id));
    }

    public Recipe create(RecipeInput input, Actor actor) {
        if (!canCreate(actor)) {
            throw new OperationNotPermittedException("Only authors and admins can create recipes");
        }
        Recipe recipe = new Recipe();
        input.applyTo(recipe);
        recipe.setStatus(RecipeStatus.DRAFT);
        recipe.setCreatedBy(actor.username());
        Recipe saved = recipeRepository.save(recipe);
        log.info("Recipe {} '{}' created by {}", saved.getId(), saved.getTitle(), actor.username());
        return saved;
    }

    public Recipe update(Long id, RecipeInput input, Actor actor) {
        Recipe recipe = get(id);
        if (!canEdit(recipe, actor)) {
            throw new OperationNotPermittedException(
                    "You can only edit your own recipes while they are in DRAFT");
        }
        if (input.getVersion() != null && !input.getVersion().equals(recipe.getVersion())) {
            throw new ConflictException("Recipe " + id + " was modified by someone else; reload and try again");
        }
        input.applyTo(recipe);
        Recipe saved = recipeRepository.saveAndFlush(recipe);
        log.info("Recipe {} updated by {}", id, actor.username());
        return saved;
    }

    public void delete(Long id, Actor actor) {
        Recipe recipe = get(id);
        if (!canDelete(actor)) {
            throw new OperationNotPermittedException("Only admins can delete recipes");
        }
        historyRepository.deleteByRecipeId(id);
        recipeRepository.delete(recipe);
        log.info("Recipe {} deleted by {}", id, actor.username());
    }

    /**
     * Moves a recipe through the review workflow.
     *
     * @throws ConflictException              if the action does not apply to the recipe's current status
     * @throws OperationNotPermittedException if the actor's role/ownership does not allow the action
     */
    public Recipe transition(Long id, WorkflowAction action, String comment, Actor actor) {
        Recipe recipe = get(id);
        RecipeStatus current = recipe.getStatus();
        if (action.getFrom() != current) {
            throw new ConflictException("Cannot " + action.name() + " a recipe in status " + current
                    + "; allowed from " + action.getFrom());
        }
        if (!canPerform(recipe, action, actor)) {
            throw new OperationNotPermittedException("User '" + actor.username() + "' is not allowed to "
                    + action.name() + " this recipe");
        }
        String trimmedComment = comment == null || comment.isBlank() ? null : comment.trim();
        if (trimmedComment != null && trimmedComment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comment must be at most " + MAX_COMMENT_LENGTH + " characters");
        }
        recipe.setStatus(action.getTo());
        historyRepository.save(new StatusHistory(recipe, current, action, actor.username(), trimmedComment));
        Recipe saved = recipeRepository.saveAndFlush(recipe);
        log.info("Recipe {} {} -> {} via {} by {}", id, current, action.getTo(), action, actor.username());
        return saved;
    }

    /** Actions the actor may perform on the recipe right now (drives the UI buttons). */
    public List<WorkflowAction> allowedActions(Recipe recipe, Actor actor) {
        return WorkflowAction.availableFrom(recipe.getStatus()).stream()
                .filter(action -> canPerform(recipe, action, actor))
                .toList();
    }

    // ---- permission rules (also used by the UI to decide which buttons to show) ----

    public boolean canCreate(Actor actor) {
        return actor.has(Role.AUTHOR) || actor.isAdmin();
    }

    public boolean canEdit(Recipe recipe, Actor actor) {
        if (actor.isAdmin()) {
            return true;
        }
        return actor.has(Role.AUTHOR)
                && recipe.isOwnedBy(actor.username())
                && recipe.getStatus() == RecipeStatus.DRAFT;
    }

    public boolean canDelete(Actor actor) {
        return actor.isAdmin();
    }

    public boolean canPerform(Recipe recipe, WorkflowAction action, Actor actor) {
        if (actor.isAdmin()) {
            return true;
        }
        boolean hasRole = action.getRoles().stream().anyMatch(actor::has);
        return hasRole && (!action.isOwnerOnly() || recipe.isOwnedBy(actor.username()));
    }
}
