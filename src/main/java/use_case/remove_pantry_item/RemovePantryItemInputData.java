package use_case.remove_pantry_item;

/**
 * Identifies the pantry item to remove.
 */
public final class RemovePantryItemInputData {
    private final String ingredientName;

    public RemovePantryItemInputData(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getIngredientName() {
        return ingredientName;
    }
}
