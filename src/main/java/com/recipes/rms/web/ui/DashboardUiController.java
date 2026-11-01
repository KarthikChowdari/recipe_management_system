package com.recipes.rms.web.ui;

import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.repository.RecipeSearchCriteria;
import com.recipes.rms.service.DashboardService;
import com.recipes.rms.service.RecipeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardUiController {

    private final DashboardService dashboardService;
    private final RecipeService recipeService;

    public DashboardUiController(DashboardService dashboardService, RecipeService recipeService) {
        this.dashboardService = dashboardService;
        this.recipeService = recipeService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", dashboardService.summary());
        model.addAttribute("statuses", RecipeStatus.values());
        model.addAttribute("reviewQueue", recipeService.search(
                new RecipeSearchCriteria(null, null, null, RecipeStatus.PENDING_REVIEW),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "updatedAt"))).getContent());
        return "dashboard";
    }
}
