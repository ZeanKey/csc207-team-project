package use_case.remove_pantry_item;

import entity.IngredientName;
import entity.Pantry;

/**
 * Removes a pantry item and saves the updated inventory.
 */
public final class RemovePantryItemInteractor implements RemovePantryItemInputBoundary {
    private final RemovePantryItemDataAccessInterface dataAccess;
    private final RemovePantryItemOutputBoundary presenter;

    public RemovePantryItemInteractor(RemovePantryItemDataAccessInterface dataAccess,
                                     RemovePantryItemOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(String ingredientName) {
        final IngredientName name;
        try {
            name = IngredientName.fromRawName(ingredientName);
        }
        catch (IllegalArgumentException exception) {
            presenter.prepareFailView("Ingredient name is required.");
            return;
        }

        final Pantry pantry = dataAccess.loadPantry();
        try {
            pantry.remove(name);
        }
        catch (IllegalArgumentException exception) {
            presenter.prepareFailView("Ingredient is not in the pantry.");
            return;
        }

        dataAccess.savePantry(pantry);
        presenter.prepareSuccessView();
    }
}
