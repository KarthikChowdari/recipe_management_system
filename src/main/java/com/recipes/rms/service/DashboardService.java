package com.recipes.rms.service;

import com.recipes.rms.domain.RecipeStatus;
import com.recipes.rms.repository.RecipeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    static final int TOP_N = 5;

    private final RecipeRepository recipeRepository;

    public DashboardService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public DashboardSummary summary() {
        Map<RecipeStatus, Long> byStatus = new EnumMap<>(RecipeStatus.class);
        for (RecipeStatus status : RecipeStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : recipeRepository.countByStatus()) {
            byStatus.put((RecipeStatus) row[0], ((Number) row[1]).longValue());
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        PageRequest top = PageRequest.of(0, TOP_N);
        return new DashboardSummary(total, Collections.unmodifiableMap(byStatus),
                recipeRepository.topCategories(top), recipeRepository.topCuisines(top));
    }
}
