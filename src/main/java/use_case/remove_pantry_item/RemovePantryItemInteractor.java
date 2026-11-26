package use_case.remove_pantry_item;

import entity.IngredientName;
import entity.Pantry;
import entity.PantryItem;

/**
 * Removes a pantry item and saves the updated inventory.
 */
public final class RemovePantryItemInteractor implements RemovePantryItemInputBoundary {
    private static final String INVALID_INPUT = "Ingredient name is required.";
    private static final String ITEM_NOT_FOUND = "Ingredient is not in the pantry.";

    private final RemovePantryItemDataAccessInterface dataAccess;
    private final RemovePantryItemOutputBoundary presenter;

    public RemovePantryItemInteractor(RemovePantryItemDataAccessInterface dataAccess,
                                     RemovePantryItemOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(RemovePantryItemInputData inputData) {
        if (inputData == null) {
            prepareFailure(INVALID_INPUT);
            return;
        }

        final IngredientName name;
        try {
            name = IngredientName.fromRawName(inputData.getIngredientName());
        }
        catch (IllegalArgumentException exception) {
            prepareFailure(INVALID_INPUT);
            return;
        }

        final Pantry pantry = dataAccess.loadPantry();
        final PantryItem item = pantry.find(name).orElse(null);
        if (item == null) {
            prepareFailure(ITEM_NOT_FOUND);
            return;
        }

        pantry.remove(name);
        dataAccess.savePantry(pantry);
        presenter.prepareSuccessView(new RemovePantryItemOutputData(
                item.getName().getDisplayName(), "", false));
    }

    private void prepareFailure(String message) {
        presenter.prepareFailView(new RemovePantryItemOutputData(null, message, true));
    }
}
