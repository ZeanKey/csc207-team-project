package use_case.complete_meal;

/** Input boundary for confirming that a planned meal was cooked. */
public interface CompleteMealInputBoundary {

    /** Confirms the selected meal and its pantry checklist. */
    void execute(CompleteMealInputData inputData);
}
