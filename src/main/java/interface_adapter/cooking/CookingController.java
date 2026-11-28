package interface_adapter.cooking;

import entity.IngredientName;
import use_case.complete_meal.CompleteMealInputBoundary;
import use_case.complete_meal.CompleteMealInputData;
import use_case.complete_meal.CompleteMealInputData.StockChoice;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary.PendingMeal;
import use_case.prepare_checklist.PrepareChecklistInputBoundary;

import java.util.Map;

/** Connects the Cook and History screens to their input boundaries. */
public final class CookingController {
    private final LoadUncookedMealsInputBoundary loadMeals;
    private final PrepareChecklistInputBoundary prepareChecklist;
    private final CompleteMealInputBoundary completeMeal;
    private final LoadCookingHistoryInputBoundary loadHistory;

    public CookingController(LoadUncookedMealsInputBoundary loadMeals,
                             PrepareChecklistInputBoundary prepareChecklist,
                             CompleteMealInputBoundary completeMeal,
                             LoadCookingHistoryInputBoundary loadHistory) {
        this.loadMeals = loadMeals;
        this.prepareChecklist = prepareChecklist;
        this.completeMeal = completeMeal;
        this.loadHistory = loadHistory;
    }

    public void loadMeals() {
        loadMeals.execute();
    }

    public void prepareChecklist(PendingMeal meal) {
        prepareChecklist.execute(meal.weekStart(), meal.slot(), meal.recipeId());
    }

    public void completeMeal(PendingMeal meal, Map<IngredientName, StockChoice> choices) {
        completeMeal.execute(new CompleteMealInputData(
                meal.weekStart(), meal.slot(), meal.recipeId(), choices));
    }

    public void loadHistory() {
        loadHistory.execute();
    }
}
