package use_case.complete_meal;

/** The message to show and whether the cooking view should reload its data. */
public record CompleteMealOutputData(String message, boolean refreshRequired) {
}
