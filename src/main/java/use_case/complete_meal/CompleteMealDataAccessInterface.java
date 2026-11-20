package use_case.complete_meal;

import entity.CookingRecord;
import entity.MealPlan;
import entity.Pantry;

import java.time.LocalDate;

/** Reads and commits the data involved in confirming a planned meal. */
public interface CompleteMealDataAccessInterface {

    /** The plan and pantry used to validate a completion. The plan may be absent. */
    record Snapshot(MealPlan mealPlan, Pantry pantry) {
    }

    /**
     * Returns a read-only view of the plan for the requested week and the current pantry.
     * Callers treat the returned entities as snapshots and build replacements for writes.
     */
    Snapshot loadSnapshot(LocalDate weekStart);

    /**
     * Saves both updates and appends the history record as one operation.
     * Returns false without saving if the snapshot has changed or the week and slot
     * already have a history entry. A storage failure also leaves all three unchanged
     * and is reported with an IllegalStateException.
     */
    boolean saveCompletion(Snapshot original, MealPlan updatedPlan, Pantry updatedPantry,
                           CookingRecord record);
}
