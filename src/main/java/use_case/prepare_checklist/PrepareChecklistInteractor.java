package use_case.prepare_checklist;

import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.Pantry;
import entity.PantryItem;
import entity.PlannedMeal;
import entity.RecipeIngredient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the current meal and fridge before showing the confirmation dialog. */
public final class PrepareChecklistInteractor implements PrepareChecklistInputBoundary {
    private final DataAccess dataAccess;
    private final OutputBoundary presenter;

    public PrepareChecklistInteractor(DataAccess dataAccess, OutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(LocalDate weekStart, MealSlot slot, String recipeId) {
        final PlannedMeal meal;
        final Pantry pantry;
        try {
            final MealPlan plan = dataAccess.loadMealPlan(weekStart);
            meal = plan == null ? null : plan.getMeals().get(slot);
            if (meal == null || meal.isCooked() || !meal.getRecipeId().equals(recipeId)) {
                presenter.showChecklistError("The selected meal changed. Select it again.", true);
                return;
            }
            pantry = dataAccess.loadPantry();
        }
        catch (IllegalStateException exception) {
            presenter.showChecklistError("Could not load the checklist. Try again.", false);
            return;
        }

        final Map<IngredientName, ChecklistItem> items = new LinkedHashMap<>();
        for (RecipeIngredient ingredient : meal.getIngredients()) {
            final IngredientName name = ingredient.getName();
            final String measure = ingredient.getMeasureDescription();
            final ChecklistItem previous = items.get(name);
            items.put(name, new ChecklistItem(name,
                    previous == null ? measure : combineMeasures(previous.measure(), measure),
                    pantry.find(name).map(PantryItem::getStockLevel).orElse(null)));
        }
        presenter.showChecklist(List.copyOf(items.values()));
    }

    private String combineMeasures(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + " + " + second;
    }
}
