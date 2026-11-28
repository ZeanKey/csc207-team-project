package use_case.complete_meal;

import entity.CookingRecord;
import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.Pantry;
import entity.PantryItem;
import entity.PlannedMeal;
import entity.RecipeIngredient;
import entity.StockLevel;
import use_case.complete_meal.CompleteMealDataAccessInterface.Snapshot;
import use_case.complete_meal.CompleteMealInputData.StockChoice;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Confirms one pending meal, updates its pantry ingredients, and records it in history. */
public final class CompleteMealInteractor implements CompleteMealInputBoundary {
    private static final String STALE_MEAL = "The selected meal changed. Select it again.";
    private static final String STALE_CHECKLIST = "Check the pantry choices again.";
    private static final String ALREADY_COOKED = "This meal has already been cooked.";
    private static final String SAVE_FAILED = "Could not save the cooking completion. Try again.";

    private final CompleteMealDataAccessInterface dataAccess;
    private final CompleteMealOutputBoundary presenter;
    private final Clock clock;

    /** Creates the interactor with its data access, output boundary, and clock. */
    public CompleteMealInteractor(CompleteMealDataAccessInterface dataAccess,
                                  CompleteMealOutputBoundary presenter, Clock clock) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.clock = clock;
    }

    @Override
    public void execute(CompleteMealInputData inputData) {
        final LocalDate today = LocalDate.now(clock);
        final LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (inputData == null || inputData.slot() == null
                || !weekStart.equals(inputData.weekStart())) {
            fail(STALE_MEAL, true);
            return;
        }

        final Snapshot snapshot;
        try {
            snapshot = dataAccess.loadSnapshot(weekStart);
        }
        catch (IllegalStateException exception) {
            fail("Could not load the meal and pantry. Try again.", false);
            return;
        }

        final PlannedMeal meal = snapshot.mealPlan() == null ? null
                : snapshot.mealPlan().getMeals().get(inputData.slot());
        if (meal == null || !meal.getRecipeId().equals(inputData.recipeId())) {
            fail(STALE_MEAL, true);
            return;
        }
        if (meal.isCooked()) {
            fail(ALREADY_COOKED, true);
            return;
        }

        final Map<IngredientName, StockChoice> choices = inputData.stockChoices();
        if (!pantryIngredients(meal, snapshot.pantry()).equals(choices.keySet())) {
            fail(STALE_CHECKLIST, true);
            return;
        }

        final MealPlan updatedPlan = withCookedMeal(snapshot.mealPlan(), inputData.slot(), meal);
        final Pantry updatedPantry = withStockChoices(snapshot.pantry(), choices);
        final CookingRecord record = new CookingRecord(
                meal.getRecipeId(), meal.getRecipeName(), today, weekStart, inputData.slot());
        final boolean saved;
        try {
            saved = dataAccess.saveCompletion(snapshot, updatedPlan, updatedPantry, record);
        }
        catch (CompleteMealDataAccessInterface.SlotAlreadyRecordedException exception) {
            fail("Cooking was already recorded for this slot. Move this meal to another slot.", false);
            return;
        }
        catch (CompleteMealDataAccessInterface.RecoveryFailedException exception) {
            fail("Could not restore all changes. Check Meal Plan and Fridge before trying again.", true);
            return;
        }
        catch (IllegalStateException exception) {
            fail(SAVE_FAILED, false);
            return;
        }

        if (saved) {
            presenter.prepareSuccessView(new CompleteMealOutputData(
                    "Cooked " + meal.getRecipeName() + ".", true));
        }
        else {
            fail("The meal or pantry changed. Reload and try again.", true);
        }
    }

    private Set<IngredientName> pantryIngredients(PlannedMeal meal, Pantry pantry) {
        final Set<IngredientName> names = new HashSet<>();
        for (RecipeIngredient ingredient : meal.getIngredients()) {
            final IngredientName name = ingredient.getName();
            if (pantry.find(name).isPresent()) {
                names.add(name);
            }
        }
        return names;
    }

    private MealPlan withCookedMeal(MealPlan original, MealSlot slot, PlannedMeal meal) {
        final PlannedMeal cooked = new PlannedMeal(
                meal.getRecipeId(), meal.getRecipeName(), meal.getIngredients());
        cooked.markCooked();
        final MealPlan updated = new MealPlan(original.getWeekStart());
        original.getMeals().forEach((key, planned) ->
                updated.assign(key, key.equals(slot) ? cooked : planned));
        return updated;
    }

    private Pantry withStockChoices(Pantry original, Map<IngredientName, StockChoice> choices) {
        final Pantry updated = new Pantry();
        for (PantryItem item : original.sortedByExpiry()) {
            final IngredientName name = item.getName();
            final StockChoice choice = choices.get(name);
            if (choice != StockChoice.USED_UP) {
                final StockLevel level;
                if (choice == null) {
                    level = item.getStockLevel();
                }
                else if (choice == StockChoice.RUNNING_LOW) {
                    level = StockLevel.LOW;
                }
                else {
                    level = StockLevel.ENOUGH;
                }
                updated.add(new PantryItem(name, item.getExpiryDate(), level));
            }
        }
        return updated;
    }

    private void fail(String message, boolean refreshRequired) {
        presenter.prepareFailView(new CompleteMealOutputData(message, refreshRequired));
    }
}
