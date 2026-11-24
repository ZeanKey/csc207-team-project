package use_case.search_recipes;

import entity.Recipe;
import java.util.List;

/**
 * Complete recipe snapshots returned by a successful search.
 */
public final class SearchRecipesOutputData {
    private final List<Recipe> recipes;

    /**
     * Creates output data from the matching recipes.
     *
     * @param recipes the matching recipe snapshots
     * @throws NullPointerException if the list or one of its elements is null
     */
    public SearchRecipesOutputData(List<Recipe> recipes) {
        this.recipes = List.copyOf(recipes);
    }

    /**
     * Returns the matching recipes.
     *
     * @return an unmodifiable list, empty when there are no matches
     */
    public List<Recipe> getRecipes() {
        return recipes;
    }
}
