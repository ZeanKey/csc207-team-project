package use_case.search_recipes;

/**
 * Input boundary for searching recipes.
 */
public interface SearchRecipesInputBoundary {
    /**
     * Executes a recipe search.
     *
     * @param inputData the search request
     */
    void execute(SearchRecipesInputData inputData);
}
