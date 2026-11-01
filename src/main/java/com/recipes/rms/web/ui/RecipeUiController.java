package com.recipes.rms.web.ui;

import com.recipes.rms.domain.Recipe;
import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.domain.WorkflowAction;
import com.recipes.rms.repository.RecipeSearchCriteria;
import com.recipes.rms.service.Actor;
import com.recipes.rms.service.ConflictException;
import com.recipes.rms.service.OperationNotPermittedException;
import com.recipes.rms.service.RecipeInput;
import com.recipes.rms.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recipes")
public class RecipeUiController {

    private static final String FORM_VIEW = "recipes/form";

    private final RecipeService recipeService;

    public RecipeUiController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public String list(@ModelAttribute("criteria") RecipeSearchCriteria criteria,
                       @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("page", recipeService.search(criteria, pageable));
        model.addAttribute("cuisines", recipeService.cuisines());
        model.addAttribute("categories", recipeService.categories());
        model.addAttribute("statuses", RecipeStatus.values());
        return "recipes/list";
    }

    @GetMapping("/new")
    public String newForm(Model model, Authentication auth) {
        if (!recipeService.canCreate(Actor.from(auth))) {
            throw new OperationNotPermittedException("Only authors and admins can create recipes");
        }
        model.addAttribute("recipeForm", new RecipeInput());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("recipeForm") RecipeInput form, BindingResult binding,
                         Authentication auth, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        Recipe created = recipeService.create(form, Actor.from(auth));
        redirect.addFlashAttribute("success", "Recipe \"" + created.getTitle() + "\" created as draft.");
        return "redirect:/recipes/" + created.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        Recipe recipe = recipeService.get(id);
        Actor actor = Actor.from(auth);
        model.addAttribute("recipe", recipe);
        model.addAttribute("canEdit", recipeService.canEdit(recipe, actor));
        model.addAttribute("canDelete", recipeService.canDelete(actor));
        model.addAttribute("allowedActions", recipeService.allowedActions(recipe, actor));
        model.addAttribute("history", recipeService.history(id));
        return "recipes/detail";
    }

    @PostMapping("/{id}/transition")
    public String transition(@PathVariable Long id, @RequestParam WorkflowAction action,
                             @RequestParam(required = false) String comment,
                             Authentication auth, RedirectAttributes redirect) {
        try {
            Recipe recipe = recipeService.transition(id, action, comment, Actor.from(auth));
            redirect.addFlashAttribute("success", action.getLabel() + " done. Status is now "
                    + recipe.getStatus().getLabel() + ".");
        } catch (ConflictException | OperationNotPermittedException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/recipes/" + id;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication auth) {
        Recipe recipe = recipeService.get(id);
        if (!recipeService.canEdit(recipe, Actor.from(auth))) {
            throw new OperationNotPermittedException("You can only edit your own recipes while they are in DRAFT");
        }
        model.addAttribute("recipeId", id);
        model.addAttribute("recipeForm", RecipeInput.from(recipe));
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("recipeForm") RecipeInput form,
                         BindingResult binding, Model model, Authentication auth, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("recipeId", id);
            return FORM_VIEW;
        }
        recipeService.update(id, form, Actor.from(auth));
        redirect.addFlashAttribute("success", "Recipe updated.");
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        recipeService.delete(id, Actor.from(auth));
        redirect.addFlashAttribute("success", "Recipe deleted.");
        return "redirect:/recipes";
    }
}
