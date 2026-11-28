package use_case.prepare_checklist;

import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.Pantry;
import entity.StockLevel;

import java.time.LocalDate;
import java.util.List;

/** Prepares the ingredient choices for a selected meal. */
public interface PrepareChecklistInputBoundary {
    void execute(LocalDate weekStart, MealSlot slot, String recipeId);

    interface DataAccess {
        MealPlan loadMealPlan(LocalDate weekStart);
        Pantry loadPantry();
    }

    interface OutputBoundary {
        void showChecklist(List<ChecklistItem> items);
        void showChecklistError(String message, boolean refreshRequired);
    }

    /** An absent stock level means the ingredient is outside the fridge. */
    record ChecklistItem(IngredientName name, String measure, StockLevel stockLevel) {
        public boolean inPantry() {
            return stockLevel != null;
        }
    }
}
