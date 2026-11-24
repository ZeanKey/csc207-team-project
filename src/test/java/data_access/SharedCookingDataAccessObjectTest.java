package data_access;

import entity.CookingRecord;
import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.MealType;
import entity.Pantry;
import entity.PantryItem;
import entity.PlannedMeal;
import entity.RecipeIngredient;
import entity.StockLevel;
import interface_adapter.cooking.CookingPresenter;
import interface_adapter.cooking.CookingViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.complete_meal.CompleteMealDataAccessInterface;
import use_case.complete_meal.CompleteMealInputData;
import use_case.complete_meal.CompleteMealInputData.StockChoice;
import use_case.complete_meal.CompleteMealInteractor;
import use_case.move_planned_meal.MovePlannedMealDataAccessInterface;
import use_case.update_stock_level.UpdateStockLevelDataAccessInterface;

import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedCookingDataAccessObjectTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T17:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 23);
    private static final MealSlot DINNER = new MealSlot(DayOfWeek.TUESDAY, MealType.DINNER);
    private static final IngredientName GARLIC = IngredientName.fromRawName("Garlic");
    private static final CompleteMealInputData CONFIRM =
            new CompleteMealInputData(WEEK, DINNER, "pasta", Map.of(GARLIC, StockChoice.USED_UP));

    @TempDir
    Path directory;

    private final CookingViewModel model = new CookingViewModel();
    private final InMemoryMealPlanDataAccessObject plans = new InMemoryMealPlanDataAccessObject();
    private FilePantryDataAccessObject pantry;
    private FileCookingLogDataAccessObject history;

    @BeforeEach
    void setUp() {
        pantry = new FilePantryDataAccessObject(directory.resolve("pantry.json"));
        history = new FileCookingLogDataAccessObject(directory.resolve("cooking_log.json"));
        final Pantry initialPantry = new Pantry();
        initialPantry.add(new PantryItem(GARLIC, null, StockLevel.ENOUGH));
        pantry.savePantry(initialPantry);

        final MealPlan plan = new MealPlan(WEEK);
        plan.assign(DINNER, new PlannedMeal("pasta", "Pasta", List.of(new RecipeIngredient(GARLIC, "1 clove"))));
        plans.saveMealPlan(plan);
    }

    @Test
    void savesToTheTeamPlanAndPantryAndReopensHistory() {
        interactor(pantry, history).execute(CONFIRM);

        assertTrue(plans.getMealPlan(WEEK).getMeals().get(DINNER).isCooked());
        assertTrue(pantry.loadPantry().find(GARLIC).isEmpty());
        final FileCookingLogDataAccessObject reopened =
                new FileCookingLogDataAccessObject(directory.resolve("cooking_log.json"));
        assertEquals(List.of(new CookingRecord("pasta", "Pasta", LocalDate.now(CLOCK), WEEK, DINNER)),
                reopened.loadHistory());
        assertEquals("Cooked Pasta.", model.getStatus().message());
    }

    @Test
    void restoresThePlanWhenItsWriteFailsAfterChangingData() {
        final MovePlannedMealDataAccessInterface plansWithFailedWrite = new MovePlannedMealDataAccessInterface() {
            @Override
            public MealPlan getMealPlan(LocalDate weekStart) {
                return plans.getMealPlan(weekStart);
            }

            @Override
            public void saveMealPlan(MealPlan updatedPlan) {
                plans.saveMealPlan(updatedPlan);
                if (updatedPlan.getMeals().get(DINNER).isCooked()) {
                    throw new IllegalStateException("Failed after writing meal plan");
                }
            }
        };
        final SharedCookingDataAccessObject store = new SharedCookingDataAccessObject(
                plansWithFailedWrite, pantry, history);

        new CompleteMealInteractor(store, new CookingPresenter(model), CLOCK).execute(CONFIRM);

        assertEquals("Could not save the cooking completion. Try again.", model.getStatus().message());
        assertOriginalData();
    }

    @Test
    void restoresPlanAndPantryWhenHistoryCannotBeWritten() {
        // A file where the history directory should be makes the append fail.
        final Path pantryFile = directory.resolve("pantry.json");
        final FileCookingLogDataAccessObject unavailableHistory =
                new FileCookingLogDataAccessObject(pantryFile.resolve("cooking_log.json"));

        interactor(pantry, unavailableHistory).execute(CONFIRM);

        assertEquals("Could not save the cooking completion. Try again.", model.getStatus().message());
        assertOriginalData();
        interactor(pantry, history).execute(CONFIRM);
        assertEquals(1, history.loadHistory().size());
    }

    @Test
    void restoresBothStoresWhenPantryWriteFailsAfterChangingData() {
        final UpdateStockLevelDataAccessInterface pantryWithFailedWrite = new UpdateStockLevelDataAccessInterface() {
            @Override
            public Pantry loadPantry() {
                return pantry.loadPantry();
            }

            @Override
            public void savePantry(Pantry updatedPantry) {
                pantry.savePantry(updatedPantry);
                if (updatedPantry.find(GARLIC).isEmpty()) {
                    throw new IllegalStateException("Failed after writing pantry");
                }
            }
        };

        interactor(pantryWithFailedWrite, history).execute(CONFIRM);

        assertEquals("Could not save the cooking completion. Try again.", model.getStatus().message());
        assertOriginalData();
    }

    @Test
    void reportsFailedRecoveryAndStillAttemptsToRestoreThePlan() {
        final UpdateStockLevelDataAccessInterface pantryWithFailedRestore = new UpdateStockLevelDataAccessInterface() {
            @Override
            public Pantry loadPantry() {
                return pantry.loadPantry();
            }

            @Override
            public void savePantry(Pantry updatedPantry) {
                if (updatedPantry.find(GARLIC).isPresent()) {
                    throw new IllegalStateException("Could not restore pantry");
                }
                pantry.savePantry(updatedPantry);
            }
        };
        final FileCookingLogDataAccessObject unavailableHistory =
                new FileCookingLogDataAccessObject(directory.resolve("pantry.json").resolve("cooking_log.json"));

        interactor(pantryWithFailedRestore, unavailableHistory).execute(CONFIRM);

        assertEquals("Could not restore all changes. Check Meal Plan and Fridge before trying again.",
                model.getStatus().message());
        assertTrue(model.getStatus().reloadMeals());
        assertFalse(plans.getMealPlan(WEEK).getMeals().get(DINNER).isCooked());
        assertTrue(pantry.loadPantry().find(GARLIC).isEmpty());
        assertTrue(history.loadHistory().isEmpty());
    }

    @Test
    void explainsWhenAReplacementMealUsesAnAlreadyRecordedSlot() {
        interactor(pantry, history).execute(CONFIRM);
        final MealPlan plan = plans.getMealPlan(WEEK);
        plan.remove(DINNER);
        plan.assign(DINNER, new PlannedMeal("soup", "Soup", List.of()));

        interactor(pantry, history).execute(new CompleteMealInputData(WEEK, DINNER, "soup", Map.of()));

        assertEquals("Cooking was already recorded for this slot. Move this meal to another slot.",
                model.getStatus().message());
        assertFalse(plan.getMeals().get(DINNER).isCooked());
        assertEquals(1, history.loadHistory().size());
    }

    @Test
    void rejectsAPlanChangedAfterReadingTheSnapshot() {
        final SharedCookingDataAccessObject store = new SharedCookingDataAccessObject(plans, pantry, history);
        final CompleteMealDataAccessInterface.Snapshot snapshot = store.loadSnapshot(WEEK);
        plans.getMealPlan(WEEK).remove(DINNER);

        final CookingRecord record = new CookingRecord("pasta", "Pasta", LocalDate.now(CLOCK), WEEK, DINNER);
        assertFalse(store.saveCompletion(snapshot, new MealPlan(WEEK), new Pantry(), record));

        assertFalse(plans.getMealPlan(WEEK).getMeals().containsKey(DINNER));
        assertTrue(pantry.loadPantry().find(GARLIC).isPresent());
        assertTrue(history.loadHistory().isEmpty());
    }

    private CompleteMealInteractor interactor(UpdateStockLevelDataAccessInterface pantryStore,
                                               FileCookingLogDataAccessObject historyStore) {
        return new CompleteMealInteractor(new SharedCookingDataAccessObject(plans, pantryStore, historyStore),
                new CookingPresenter(model), CLOCK);
    }

    private void assertOriginalData() {
        assertFalse(plans.getMealPlan(WEEK).getMeals().get(DINNER).isCooked());
        assertTrue(pantry.loadPantry().find(GARLIC).isPresent());
        assertTrue(history.loadHistory().isEmpty());
    }
}
