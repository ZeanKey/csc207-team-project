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
     * Returns false without saving if the snapshot has changed. An existing history
     * entry for the week and slot raises SlotAlreadyRecordedException. A normal
     * storage failure leaves the original state intact. RecoveryFailedException
     * indicates that a saved change could not be undone.
     */
    boolean saveCompletion(Snapshot original, MealPlan updatedPlan, Pantry updatedPantry,
                           CookingRecord record);

    /** A different meal has already been recorded in the same week and slot. */
    final class SlotAlreadyRecordedException extends IllegalStateException {
    }

    /** The plan or fridge may still contain changes from a failed completion. */
    final class RecoveryFailedException extends IllegalStateException {
        public RecoveryFailedException(IllegalStateException cause) {
            super("Could not restore Cooking data after a failed save.", cause);
        }
    }
}
