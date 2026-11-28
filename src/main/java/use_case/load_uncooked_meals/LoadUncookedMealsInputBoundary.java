package use_case.load_uncooked_meals;

import entity.MealPlan;
import entity.MealSlot;

import java.time.LocalDate;
import java.util.List;

/** Loads the pending meals for the Cook page. */
public interface LoadUncookedMealsInputBoundary {
    void execute();

    interface DataAccess {
        MealPlan loadMealPlan(LocalDate weekStart);
    }

    interface OutputBoundary {
        void showMeals(List<PendingMeal> meals);
        void showMealsError(String message);
    }

    /** Identifies the selected meal and provides its display name. */
    record PendingMeal(LocalDate weekStart, MealSlot slot, String recipeId, String recipeName) {
    }
}
