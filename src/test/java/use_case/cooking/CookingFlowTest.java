package use_case.cooking;

import data_access.FileCookingDataAccessObject;
import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.MealType;
import entity.Pantry;
import entity.PantryItem;
import entity.PlannedMeal;
import entity.RecipeIngredient;
import entity.StockLevel;
import interface_adapter.cooking.CookingController;
import interface_adapter.cooking.CookingPresenter;
import interface_adapter.cooking.CookingViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.complete_meal.CompleteMealInputData.StockChoice;
import use_case.complete_meal.CompleteMealInteractor;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary.HistoryEntry;
import use_case.load_cooking_history.LoadCookingHistoryInteractor;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary.PendingMeal;
import use_case.load_uncooked_meals.LoadUncookedMealsInteractor;
import use_case.prepare_checklist.PrepareChecklistInputBoundary.ChecklistItem;
import use_case.prepare_checklist.PrepareChecklistInteractor;

import java.io.IOException;
import java.nio.file.Files;
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

class CookingFlowTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T17:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 16);
    private static final MealSlot LUNCH = new MealSlot(DayOfWeek.WEDNESDAY, MealType.LUNCH);
    private static final MealSlot DINNER = new MealSlot(DayOfWeek.WEDNESDAY, MealType.DINNER);
    private static final MealSlot COOKED_SLOT = new MealSlot(DayOfWeek.MONDAY, MealType.DINNER);
    private static final IngredientName GARLIC = IngredientName.fromRawName("Garlic");
    private static final IngredientName TOMATO = IngredientName.fromRawName("Tomato");

    @TempDir
    Path folder;

    private FileCookingDataAccessObject store;
    private CookingViewModel model;
    private CookingController controller;

    @BeforeEach
    void setUp() {
        final MealPlan plan = new MealPlan(WEEK_START);
        final PlannedMeal cooked = new PlannedMeal("cooked", "Rice", List.of());
        cooked.markCooked();
        plan.assign(DINNER, new PlannedMeal("soup", "Soup", List.of()));
        plan.assign(COOKED_SLOT, cooked);
        plan.assign(LUNCH, new PlannedMeal("pasta", "Pasta", List.of(
                new RecipeIngredient(GARLIC, "1 clove"),
                new RecipeIngredient(IngredientName.fromRawName("garlic"), "2 cloves"),
                new RecipeIngredient(TOMATO, "1"))));
        final Pantry pantry = new Pantry();
        pantry.add(new PantryItem(GARLIC, null, StockLevel.LOW));
        store = new FileCookingDataAccessObject(file(), plan, pantry);
        model = new CookingViewModel();
        controller = controllerFor(store, model);
    }

    @Test
    void loadsOnlyPendingMealsInSlotOrder() {
        controller.loadMeals();

        assertEquals(List.of(LUNCH, DINNER), model.getMeals().stream().map(PendingMeal::slot).toList());
    }

    @Test
    void preparesOneStockChoicePerIngredientAndShowsMissingItems() {
        controller.prepareChecklist(lunch());

        assertEquals(List.of(new ChecklistItem(GARLIC, "1 clove + 2 cloves", StockLevel.LOW),
                new ChecklistItem(TOMATO, "1", null)), model.getChecklist());
        assertFalse(model.getChecklist().get(1).inPantry());
        assertFalse(Files.exists(file()));
    }

    @Test
    void loadsEmptyHistoryAndThenShowsTheNewestCompletionFirst() {
        controller.loadHistory();
        assertEquals("No meals cooked yet.", model.getHistoryMessage());

        controller.completeMeal(lunch(), Map.of(GARLIC, StockChoice.USED_UP));
        controller.completeMeal(new PendingMeal(WEEK_START, DINNER, "soup", "Soup"), Map.of());
        controller.loadHistory();

        final LocalDate today = LocalDate.now(CLOCK);
        assertEquals(List.of(new HistoryEntry(today, "Soup"), new HistoryEntry(today, "Pasta")),
                model.getHistory());
    }

    @Test
    void keepsCookedMealPantryAndHistoryAfterReopeningFile() {
        controller.completeMeal(lunch(), Map.of(GARLIC, StockChoice.USED_UP));

        final FileCookingDataAccessObject reopened =
                new FileCookingDataAccessObject(file(), new MealPlan(WEEK_START), new Pantry());
        final CookingViewModel reopenedModel = new CookingViewModel();
        final CookingController reopenedController = controllerFor(reopened, reopenedModel);
        reopenedController.loadMeals();
        reopenedController.loadHistory();

        assertTrue(reopened.loadMealPlan(WEEK_START).getMeals().get(LUNCH).isCooked());
        assertTrue(reopened.loadPantry().find(GARLIC).isEmpty());
        assertEquals(List.of(DINNER), reopenedModel.getMeals().stream().map(PendingMeal::slot).toList());
        assertEquals(List.of(new HistoryEntry(LocalDate.now(CLOCK), "Pasta")), reopenedModel.getHistory());
    }

    @Test
    void failedWriteLeavesStateUnchangedAndCanBeRetried() throws IOException {
        Files.createDirectory(file());
        controller.completeMeal(lunch(), Map.of(GARLIC, StockChoice.USED_UP));

        assertEquals("Could not save the cooking completion. Try again.", model.getStatus().message());
        assertFalse(store.loadMealPlan(WEEK_START).getMeals().get(LUNCH).isCooked());
        assertTrue(store.loadPantry().find(GARLIC).isPresent());
        assertTrue(store.loadHistory().isEmpty());

        Files.delete(file());
        controller.completeMeal(lunch(), Map.of(GARLIC, StockChoice.USED_UP));
        assertTrue(store.loadMealPlan(WEEK_START).getMeals().get(LUNCH).isCooked());
        assertEquals(1, store.loadHistory().size());
    }

    private Path file() {
        return folder.resolve("cooking.json");
    }

    private static PendingMeal lunch() {
        return new PendingMeal(WEEK_START, LUNCH, "pasta", "Pasta");
    }

    private static CookingController controllerFor(FileCookingDataAccessObject store,
                                                   CookingViewModel model) {
        final CookingPresenter presenter = new CookingPresenter(model);
        return new CookingController(
                new LoadUncookedMealsInteractor(store, presenter, CLOCK),
                new PrepareChecklistInteractor(store, presenter),
                new CompleteMealInteractor(store, presenter, CLOCK),
                new LoadCookingHistoryInteractor(store, presenter));
    }
}
