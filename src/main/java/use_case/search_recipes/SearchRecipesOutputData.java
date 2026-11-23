package use_case.search_recipes;

import entity.Recipe;
import java.util.List;

/** Complete recipe snapshots returned by a successful search. */
public final class SearchRecipesOutputData {
    private final List<Recipe> recipes;

    public SearchRecipesOutputData(List<Recipe> recipes) {
        this.recipes = List.copyOf(recipes);
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }
}
