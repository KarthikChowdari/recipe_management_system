package com.recipes.rms.web.api;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.repository.RecipeSearchCriteria;
import com.recipes.rms.service.Actor;
import com.recipes.rms.service.RecipeInput;
import com.recipes.rms.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeApiController {

    private final RecipeService recipeService;

    public RecipeApiController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /** Search: every query parameter ({@code title, cuisine, category, status}) is optional. */
    @GetMapping
    public PageResponse<RecipeResponse> search(
            @ModelAttribute RecipeSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(recipeService.search(criteria, pageable), RecipeResponse::from);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable Long id) {
        return RecipeResponse.from(recipeService.get(id));
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeInput input, Authentication auth) {
        Recipe created = recipeService.create(input, Actor.from(auth));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(RecipeResponse.from(created));
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id, @Valid @RequestBody RecipeInput input, Authentication auth) {
        return RecipeResponse.from(recipeService.update(id, input, Actor.from(auth)));
    }

    @PostMapping("/{id}/transitions")
    public RecipeResponse transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest request,
                                     Authentication auth) {
        return RecipeResponse.from(
                recipeService.transition(id, request.action(), request.comment(), Actor.from(auth)));
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryResponse> history(@PathVariable Long id) {
        return recipeService.history(id).stream().map(StatusHistoryResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        recipeService.delete(id, Actor.from(auth));
        return ResponseEntity.noContent().build();
    }
}
