package use_case.remove_pantry_item;

import entity.Pantry;

/**
 * Loads and saves the pantry for item removal.
 */
public interface RemovePantryItemDataAccessInterface {
    Pantry loadPantry();

    void savePantry(Pantry pantry);
}
