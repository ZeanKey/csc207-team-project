package use_case.remove_pantry_item;

/**
 * Removes the pantry item identified by its ingredient name.
 */
public interface RemovePantryItemInputBoundary {
    void execute(String ingredientName);
}
