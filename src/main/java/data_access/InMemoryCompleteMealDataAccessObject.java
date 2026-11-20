package data_access;

import entity.CookingRecord;
import entity.MealPlan;
import entity.Pantry;
import use_case.complete_meal.CompleteMealDataAccessInterface;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Stores a weekly plan, pantry, and cooking history for an in-memory prototype. */
public final class InMemoryCompleteMealDataAccessObject implements CompleteMealDataAccessInterface {
    private final List<CookingRecord> history = new ArrayList<>();
    private MealPlan mealPlan;
    private Pantry pantry;

    /** Seeds the in-memory store with a plan and pantry. */
    public InMemoryCompleteMealDataAccessObject(MealPlan mealPlan, Pantry pantry) {
        this.mealPlan = mealPlan;
        this.pantry = pantry;
    }

    @Override
    public synchronized Snapshot loadSnapshot(LocalDate weekStart) {
        final MealPlan requestedPlan = mealPlan.getWeekStart().equals(weekStart) ? mealPlan : null;
        return new Snapshot(requestedPlan, pantry);
    }

    @Override
    public synchronized boolean saveCompletion(Snapshot original, MealPlan updatedPlan,
                                               Pantry updatedPantry, CookingRecord record) {
        if (original.mealPlan() != mealPlan || original.pantry() != pantry
                || history.stream().anyMatch(entry -> entry.weekStart().equals(record.weekStart())
                        && entry.slot().equals(record.slot()))) {
            return false;
        }

        history.add(record);
        mealPlan = updatedPlan;
        pantry = updatedPantry;
        return true;
    }

    /** Returns the records saved by this instance. */
    public synchronized List<CookingRecord> getCookingHistory() {
        return List.copyOf(history);
    }
}
