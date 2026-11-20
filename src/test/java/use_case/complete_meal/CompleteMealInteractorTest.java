package use_case.complete_meal;

import data_access.InMemoryCompleteMealDataAccessObject;
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
import org.junit.jupiter.api.Test;
import use_case.complete_meal.CompleteMealInputData.StockChoice;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteMealInteractorTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-20T17:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 16);
    private static final MealSlot DINNER = new MealSlot(DayOfWeek.FRIDAY, MealType.DINNER);
    private static final MealSlot LUNCH = new MealSlot(DayOfWeek.SATURDAY, MealType.LUNCH);
    private static final String RECIPE_ID = "123";
    private static final IngredientName MILK = IngredientName.fromRawName("Milk");
    private static final IngredientName GARLIC = IngredientName.fromRawName("Garlic");
    private static final IngredientName FLOUR = IngredientName.fromRawName("Flour");
    private static final IngredientName SALT = IngredientName.fromRawName("Salt");
    private static final IngredientName ONION = IngredientName.fromRawName("Onion");
    private static final Map<IngredientName, StockChoice> CHOICES = Map.of(
            IngredientName.fromRawName("milk"), StockChoice.USED_UP,
            GARLIC, StockChoice.RUNNING_LOW,
            FLOUR, StockChoice.STILL_ENOUGH);

    private final MealPlan plan = new MealPlan(WEEK);
    private final Pantry pantry = new Pantry();
    private final InMemoryCompleteMealDataAccessObject store =
            new InMemoryCompleteMealDataAccessObject(plan, pantry);
    private final TestPresenter presenter = new TestPresenter();
    private final CompleteMealInteractor interactor = new CompleteMealInteractor(store, presenter, CLOCK);

    CompleteMealInteractorTest() {
        plan.assign(DINNER, new PlannedMeal(RECIPE_ID, "Garlic sauce", List.of(
                ingredient(MILK), ingredient(GARLIC), ingredient(FLOUR), ingredient(SALT))));
        plan.assign(LUNCH, new PlannedMeal(RECIPE_ID, "Garlic sauce", List.of(ingredient(SALT))));
        pantry.add(new PantryItem(MILK, null, StockLevel.ENOUGH));
        pantry.add(new PantryItem(GARLIC, WEEK, StockLevel.ENOUGH));
        pantry.add(new PantryItem(FLOUR, null, StockLevel.LOW));
        pantry.add(new PantryItem(ONION, null, StockLevel.LOW));
    }

    @Test
    void completesMealAndSavesPantryAndHistoryTogether() {
        interactor.execute(dinnerInput());

        final CompleteMealDataAccessInterface.Snapshot saved = store.loadSnapshot(WEEK);
        assertTrue(saved.mealPlan().getMeals().get(DINNER).isCooked());
        assertEquals(List.of(saved.mealPlan().getMeals().get(LUNCH)), saved.mealPlan().uncookedMeals());
        assertTrue(saved.pantry().find(MILK).isEmpty());
        assertTrue(saved.pantry().find(SALT).isEmpty());
        assertEquals(StockLevel.LOW, saved.pantry().find(GARLIC).orElseThrow().getStockLevel());
        assertEquals(WEEK, saved.pantry().find(GARLIC).orElseThrow().getExpiryDate());
        assertEquals(StockLevel.ENOUGH, saved.pantry().find(FLOUR).orElseThrow().getStockLevel());
        assertEquals(StockLevel.LOW, saved.pantry().find(ONION).orElseThrow().getStockLevel());
        assertEquals(List.of(new CookingRecord(RECIPE_ID, "Garlic sauce",
                LocalDate.now(CLOCK), WEEK, DINNER)), store.getCookingHistory());
        assertTrue(presenter.success.refreshRequired());
        assertNull(presenter.failure);
    }

    @Test
    void rejectsDuplicateConfirmationButAllowsSameRecipeInAnotherSlot() {
        interactor.execute(dinnerInput());
        interactor.execute(dinnerInput());

        assertEquals("This meal has already been cooked.", presenter.failure.message());
        assertTrue(presenter.failure.refreshRequired());
        assertEquals(1, store.getCookingHistory().size());

        interactor.execute(new CompleteMealInputData(WEEK, LUNCH, RECIPE_ID, Map.of()));

        assertEquals(List.of(DINNER, LUNCH), store.getCookingHistory().stream()
                .map(CookingRecord::slot).toList());
    }

    @Test
    void rejectsStaleRecipeOrWeekAndRequestsRefresh() {
        interactor.execute(new CompleteMealInputData(WEEK, DINNER, "replaced", CHOICES));
        assertTrue(presenter.failure.refreshRequired());
        interactor.execute(new CompleteMealInputData(WEEK.minusWeeks(1), DINNER, RECIPE_ID, CHOICES));
        assertTrue(presenter.failure.refreshRequired());
        assertUnchanged();
    }

    @Test
    void rejectsChoicesForIngredientsMissingFromThePantry() {
        interactor.execute(new CompleteMealInputData(WEEK, DINNER, RECIPE_ID,
                Map.of(MILK, StockChoice.USED_UP, GARLIC, StockChoice.RUNNING_LOW,
                        FLOUR, StockChoice.STILL_ENOUGH, SALT, StockChoice.USED_UP)));

        assertEquals("Check the pantry choices again.", presenter.failure.message());
        assertTrue(presenter.failure.refreshRequired());
        assertUnchanged();
    }

    @Test
    void rejectsIncompleteChecklistBeforeChangingAnything() {
        interactor.execute(new CompleteMealInputData(WEEK, DINNER, RECIPE_ID,
                Map.of(MILK, StockChoice.USED_UP)));

        assertTrue(presenter.failure.refreshRequired());
        assertUnchanged();
    }

    @Test
    void reportsStorageFailureAndCanRetryWithoutPartialChanges() {
        final CompleteMealDataAccessInterface failingStore = new CompleteMealDataAccessInterface() {
            @Override
            public Snapshot loadSnapshot(LocalDate weekStart) {
                return store.loadSnapshot(weekStart);
            }

            @Override
            public boolean saveCompletion(Snapshot original, MealPlan updatedPlan, Pantry updatedPantry,
                                          CookingRecord record) {
                throw new IllegalStateException("Storage unavailable");
            }
        };

        new CompleteMealInteractor(failingStore, presenter, CLOCK).execute(dinnerInput());

        assertFalse(presenter.failure.refreshRequired());
        assertUnchanged();

        interactor.execute(dinnerInput());
        assertEquals(1, store.getCookingHistory().size());
    }

    @Test
    void rejectsSavingAgainstAnOlderPantryAndPlanSnapshot() {
        final CompleteMealDataAccessInterface.Snapshot original = store.loadSnapshot(WEEK);
        interactor.execute(dinnerInput());

        final boolean saved = store.saveCompletion(original, plan, pantry,
                new CookingRecord(RECIPE_ID, "Garlic sauce", LocalDate.now(CLOCK), WEEK, LUNCH));

        assertFalse(saved);
        assertEquals(1, store.getCookingHistory().size());
        assertTrue(store.loadSnapshot(WEEK).mealPlan().getMeals().get(DINNER).isCooked());
        assertTrue(store.loadSnapshot(WEEK).pantry().find(MILK).isEmpty());
    }

    private void assertUnchanged() {
        final CompleteMealDataAccessInterface.Snapshot saved = store.loadSnapshot(WEEK);
        assertFalse(saved.mealPlan().getMeals().get(DINNER).isCooked());
        assertEquals(StockLevel.ENOUGH, saved.pantry().find(MILK).orElseThrow().getStockLevel());
        assertEquals(StockLevel.ENOUGH, saved.pantry().find(GARLIC).orElseThrow().getStockLevel());
        assertEquals(StockLevel.LOW, saved.pantry().find(FLOUR).orElseThrow().getStockLevel());
        assertTrue(store.getCookingHistory().isEmpty());
        assertNull(presenter.success);
    }

    private static RecipeIngredient ingredient(IngredientName name) {
        return new RecipeIngredient(name, "1 tsp");
    }

    private static CompleteMealInputData dinnerInput() {
        return new CompleteMealInputData(WEEK, DINNER, RECIPE_ID, CHOICES);
    }

    private static final class TestPresenter implements CompleteMealOutputBoundary {
        private CompleteMealOutputData success;
        private CompleteMealOutputData failure;

        @Override
        public void prepareSuccessView(CompleteMealOutputData outputData) {
            success = outputData;
            failure = null;
        }

        @Override
        public void prepareFailView(CompleteMealOutputData outputData) {
            failure = outputData;
            success = null;
        }
    }
}
