package use_case.ingredient_catalog;

import java.util.List;
import java.util.Optional;

import entity.IngredientName;

/**
 * Provides the known ingredient names used by pantry use cases.
 */
public interface IngredientCatalog {

    /**
     * Finds the catalog's canonical name for an ingredient.
     *
     * @param name the normalized ingredient value to look up
     * @return the catalog name, or empty when the ingredient is unknown
     */
    Optional<IngredientName> find(IngredientName name);

    /**
     * Returns known ingredients whose normalized names start with the query.
     *
     * @param query user-entered ingredient text
     * @return matching names ordered alphabetically
     */
    List<IngredientName> suggest(String query);
}
