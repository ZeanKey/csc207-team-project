package interface_adapter.load_pantry;

import use_case.load_pantry.LoadPantryInputBoundary;
import use_case.load_pantry.LoadPantryInputData;

/**
 * Loads the current inventory for the fridge page.
 */
public final class LoadPantryController {
    private final LoadPantryInputBoundary interactor;

    public LoadPantryController(LoadPantryInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void execute() {
        interactor.execute(new LoadPantryInputData());
    }
}
