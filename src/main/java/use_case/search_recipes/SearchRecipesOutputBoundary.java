package use_case.search_recipes;

/**
 * Presents search results or an actionable error.
 */
public interface SearchRecipesOutputBoundary {
    /**
     * Presents a successful search, including an empty result.
     *
     * @param outputData the successful search results
     */
    void prepareSuccessView(SearchRecipesOutputData outputData);

    /**
     * Presents a search failure.
     *
     * @param message the error message to display
     */
    void prepareFailView(String message);
}
