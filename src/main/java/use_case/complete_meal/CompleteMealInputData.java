package use_case.complete_meal;

import entity.IngredientName;
import entity.MealSlot;

import java.time.LocalDate;
import java.util.Map;

/** The meal shown in the checklist and one choice per ingredient in the pantry. */
public record CompleteMealInputData(LocalDate weekStart, MealSlot slot, String recipeId,
                                    Map<IngredientName, StockChoice> stockChoices) {

    /** The three choices available for a pantry ingredient. */
    public enum StockChoice {
        USED_UP, RUNNING_LOW, STILL_ENOUGH
    }

    /** Takes a snapshot of the choices made before confirmation. */
    public CompleteMealInputData {
        stockChoices = Map.copyOf(stockChoices);
    }
}
