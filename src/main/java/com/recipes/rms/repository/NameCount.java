package com.recipes.rms.repository;

/** Aggregation row: a grouping value (category, cuisine) and how many recipes have it. */
public record NameCount(String name, long count) {
}
