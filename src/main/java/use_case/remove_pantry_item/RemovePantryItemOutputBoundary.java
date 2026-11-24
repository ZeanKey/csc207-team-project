package use_case.remove_pantry_item;

/**
 * Reports whether a pantry item was removed.
 */
public interface RemovePantryItemOutputBoundary {
    void prepareSuccessView(RemovePantryItemOutputData outputData);

    void prepareFailView(RemovePantryItemOutputData outputData);
}
