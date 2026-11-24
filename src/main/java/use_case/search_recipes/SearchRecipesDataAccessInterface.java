package use_case.search_recipes;

import entity.Recipe;
import java.io.IOException;
import java.util.List;

/**
 * Searches a recipe source and returns complete snapshots.
 */
public interface SearchRecipesDataAccessInterface {
    /**
     * Searches for recipes by name.
     *
     * @param keyword non-null recipe name or search text
     * @return complete recipe snapshots, or an empty list if none match
     * @throws IOException if the source cannot be accessed or its response is invalid
     * @throws NullPointerException if keyword is null
     */
    List<Recipe> searchByName(String keyword) throws IOException;

    /**
     * Searches for recipes containing an ingredient.
     *
     * @param keyword non-null ingredient name
     * @return complete recipe snapshots, or an empty list if none match
     * @throws IOException if the source cannot be accessed, its response is invalid,
     *         or any matching recipe's details cannot be loaded
     * @throws NullPointerException if keyword is null
     */
    List<Recipe> searchByIngredient(String keyword) throws IOException;
}
