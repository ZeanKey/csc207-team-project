package use_case.search_recipes;

/** Presents search results or an actionable error. */
public interface SearchRecipesOutputBoundary {
    void prepareSuccessView(SearchRecipesOutputData outputData);

    void prepareFailView(String message);
}
