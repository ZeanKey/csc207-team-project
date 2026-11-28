package data_access;

import entity.CookingRecord;
import entity.MealPlan;
import entity.MealSlot;
import entity.Pantry;
import entity.PantryItem;
import entity.PlannedMeal;
import entity.RecipeIngredient;
import use_case.complete_meal.CompleteMealDataAccessInterface;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary;
import use_case.move_planned_meal.MovePlannedMealDataAccessInterface;
import use_case.prepare_checklist.PrepareChecklistInputBoundary;
import use_case.update_stock_level.UpdateStockLevelDataAccessInterface;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Connects Cooking to the same meal plan and pantry stores used by the other screens. */
public final class SharedCookingDataAccessObject implements CompleteMealDataAccessInterface,
        LoadUncookedMealsInputBoundary.DataAccess, PrepareChecklistInputBoundary.DataAccess,
        LoadCookingHistoryInputBoundary.DataAccess {
    /** History appends must leave the previous log intact if they fail. */
    public interface HistoryStore extends LoadCookingHistoryInputBoundary.DataAccess {
        void append(CookingRecord record);
    }

    private final MovePlannedMealDataAccessInterface mealPlans;
    private final UpdateStockLevelDataAccessInterface pantry;
    private final HistoryStore history;

    public SharedCookingDataAccessObject(MovePlannedMealDataAccessInterface mealPlans,
                                         UpdateStockLevelDataAccessInterface pantry, HistoryStore history) {
        this.mealPlans = mealPlans;
        this.pantry = pantry;
        this.history = history;
    }

    @Override
    public MealPlan loadMealPlan(LocalDate weekStart) {
        return mealPlans.getMealPlan(weekStart);
    }

    @Override
    public Pantry loadPantry() {
        return pantry.loadPantry();
    }

    @Override
    public List<CookingRecord> loadHistory() {
        return history.loadHistory();
    }

    @Override
    public Snapshot loadSnapshot(LocalDate weekStart) {
        // The in-memory plan DAO returns mutable entities. Keep separate copies for rollback.
        final MealPlan plan = loadMealPlan(weekStart);
        final MealPlan originalPlan = plan == null ? null : copyPlan(plan);
        final Pantry originalPantry = new Pantry();
        for (PantryItem item : loadPantry().sortedByExpiry()) {
            originalPantry.add(new PantryItem(item.getName(), item.getExpiryDate(), item.getStockLevel()));
        }
        return new Snapshot(originalPlan, originalPantry);
    }

    /**
     * Saves on the application's Swing event thread, like the other writers.
     * Restores attempted writes on failure. Recovery after a process exit is outside this contract.
     */
    @Override
    public boolean saveCompletion(Snapshot original, MealPlan updatedPlan,
                                  Pantry updatedPantry, CookingRecord record) {
        final MealPlan beforePlan = original.mealPlan();
        final Pantry beforePantry = original.pantry();
        if (beforePlan == null || !samePlan(beforePlan, loadMealPlan(record.weekStart()))
                || !samePantry(beforePantry, loadPantry())) {
            return false;
        }
        if (loadHistory().stream().anyMatch(entry -> entry.weekStart().equals(record.weekStart())
                && entry.slot().equals(record.slot()))) {
            throw new SlotAlreadyRecordedException();
        }

        boolean pantryAttempted = false;
        try {
            mealPlans.saveMealPlan(updatedPlan);
            pantryAttempted = true;
            pantry.savePantry(updatedPantry);
            history.append(record);
        }
        catch (IllegalStateException saveFailure) {
            // A write can fail after changing its file, so restore the attempted store too.
            final boolean pantryRestored = !pantryAttempted
                    || restore(() -> pantry.savePantry(beforePantry), saveFailure);
            final boolean planRestored = restore(() -> mealPlans.saveMealPlan(beforePlan), saveFailure);
            if (!pantryRestored || !planRestored) {
                throw new RecoveryFailedException(saveFailure);
            }
            throw saveFailure;
        }
        return true;
    }

    private static boolean restore(Runnable write, IllegalStateException saveFailure) {
        try {
            write.run();
            return true;
        }
        catch (IllegalStateException restoreFailure) {
            if (restoreFailure != saveFailure) {
                saveFailure.addSuppressed(restoreFailure);
            }
            return false;
        }
    }

    private static MealPlan copyPlan(MealPlan plan) {
        final MealPlan copy = new MealPlan(plan.getWeekStart());
        plan.getMeals().forEach((slot, meal) -> {
            final PlannedMeal copiedMeal = new PlannedMeal(
                    meal.getRecipeId(), meal.getRecipeName(), meal.getIngredients());
            if (meal.isCooked()) {
                copiedMeal.markCooked();
            }
            copy.assign(slot, copiedMeal);
        });
        return copy;
    }

    private static boolean samePlan(MealPlan before, MealPlan current) {
        if (current == null || !before.getWeekStart().equals(current.getWeekStart())
                || !before.getMeals().keySet().equals(current.getMeals().keySet())) {
            return false;
        }
        for (Map.Entry<MealSlot, PlannedMeal> entry : before.getMeals().entrySet()) {
            final PlannedMeal oldMeal = entry.getValue();
            final PlannedMeal newMeal = current.getMeals().get(entry.getKey());
            if (oldMeal.isCooked() != newMeal.isCooked()
                    || !oldMeal.getRecipeId().equals(newMeal.getRecipeId())
                    || !oldMeal.getRecipeName().equals(newMeal.getRecipeName())
                    || !ingredients(oldMeal).equals(ingredients(newMeal))) {
                return false;
            }
        }
        return true;
    }

    private static List<Ingredient> ingredients(PlannedMeal meal) {
        return meal.getIngredients().stream().map(Ingredient::new).toList();
    }

    private record Ingredient(String name, String measure) {
        Ingredient(RecipeIngredient ingredient) {
            this(ingredient.getDisplayName(), ingredient.getMeasureDescription());
        }
    }

    private static boolean samePantry(Pantry before, Pantry current) {
        final List<PantryItem> oldItems = before.sortedByExpiry();
        if (oldItems.size() != current.sortedByExpiry().size()) {
            return false;
        }
        for (PantryItem item : oldItems) {
            final PantryItem existing = current.find(item.getName()).orElse(null);
            if (existing == null || item.getStockLevel() != existing.getStockLevel()
                    || !item.getName().getDisplayName().equals(existing.getName().getDisplayName())
                    || !Objects.equals(item.getExpiryDate(), existing.getExpiryDate())) {
                return false;
            }
        }
        return true;
    }
}
