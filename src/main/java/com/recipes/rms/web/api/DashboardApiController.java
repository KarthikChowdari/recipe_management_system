package com.recipes.rms.web.api;

import com.recipes.rms.service.DashboardService;
import com.recipes.rms.service.DashboardSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardSummary summary() {
        return dashboardService.summary();
    }
}
