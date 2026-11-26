package use_case.complete_meal;

/** Presents the result of confirming a meal. */
public interface CompleteMealOutputBoundary {

    /** Presents a saved completion and refreshes the pending meals. */
    void prepareSuccessView(CompleteMealOutputData outputData);

    /** Presents a failure and refreshes the pending meals if requested. */
    void prepareFailView(CompleteMealOutputData outputData);
}
