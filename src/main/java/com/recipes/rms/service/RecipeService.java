package com.recipes.rms.service;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.domain.Role;
import com.recipes.rms.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public Page<Recipe> findAll(Pageable pageable) {
        return recipeRepository.findAll(pageable);
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
        recipeRepository.delete(recipe);
        log.info("Recipe {} deleted by {}", id, actor.username());
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
}
