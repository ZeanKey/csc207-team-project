package entity;

import java.time.LocalDate;

/** One completed meal, identified by its week and meal slot. */
public record CookingRecord(String recipeId, String recipeName, LocalDate cookedOn,
                            LocalDate weekStart, MealSlot slot) {
}
