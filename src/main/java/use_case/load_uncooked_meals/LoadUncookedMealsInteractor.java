package use_case.load_uncooked_meals;

import entity.MealPlan;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/** Lists the current week's uncooked meals in meal-slot order. */
public final class LoadUncookedMealsInteractor implements LoadUncookedMealsInputBoundary {
    private final DataAccess dataAccess;
    private final OutputBoundary presenter;
    private final Clock clock;

    public LoadUncookedMealsInteractor(DataAccess dataAccess, OutputBoundary presenter, Clock clock) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.clock = clock;
    }

    @Override
    public void execute() {
        final LocalDate week = LocalDate.now(clock)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final MealPlan plan;
        try {
            plan = dataAccess.loadMealPlan(week);
        }
        catch (IllegalStateException exception) {
            presenter.showMealsError("Could not load your meal plan. Try again.");
            return;
        }

        final List<PendingMeal> meals = plan == null ? List.of() : plan.getMeals().entrySet().stream()
                .filter(entry -> !entry.getValue().isCooked())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PendingMeal(week, entry.getKey(),
                        entry.getValue().getRecipeId(), entry.getValue().getRecipeName()))
                .toList();
        presenter.showMeals(meals);
    }
}
