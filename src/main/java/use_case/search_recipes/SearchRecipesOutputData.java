package use_case.search_recipes;

import java.util.List;

/**
 * The recipes found by a successful search.
 */
public final class SearchRecipesOutputData {
    private final List<RecipeSummaryOutputData> results;

    /**
     * Creates output data from the matching recipes.
     *
     * @param results one summary per matching recipe, in search order
     * @throws NullPointerException if the list or one of its elements is null
     */
    public SearchRecipesOutputData(List<RecipeSummaryOutputData> results) {
        this.results = List.copyOf(results);
    }

    /**
     * Returns the matching recipes.
     *
     * @return an unmodifiable list, empty when there are no matches
     */
    public List<RecipeSummaryOutputData> getResults() {
        return results;
    }
}
