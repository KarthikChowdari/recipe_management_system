package com.recipes.rms.repository;

import com.recipes.rms.domain.Recipe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    @Query("select distinct r.cuisine from Recipe r order by r.cuisine")
    List<String> findDistinctCuisines();

    @Query("select distinct r.category from Recipe r order by r.category")
    List<String> findDistinctCategories();

    /** Rows of {@code [RecipeStatus, Long]}. */
    @Query("select r.status, count(r) from Recipe r group by r.status")
    List<Object[]> countByStatus();

    @Query("""
            select new com.recipes.rms.repository.NameCount(r.category, count(r))
            from Recipe r group by r.category order by count(r) desc, r.category asc""")
    List<NameCount> topCategories(Pageable limit);

    @Query("""
            select new com.recipes.rms.repository.NameCount(r.cuisine, count(r))
            from Recipe r group by r.cuisine order by count(r) desc, r.cuisine asc""")
    List<NameCount> topCuisines(Pageable limit);
}
