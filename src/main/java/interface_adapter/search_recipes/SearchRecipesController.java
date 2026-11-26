package interface_adapter.search_recipes;

import use_case.search_recipes.RecipeSearchType;
import use_case.search_recipes.SearchRecipesInputBoundary;
import use_case.search_recipes.SearchRecipesInputData;

import java.util.Objects;

/**
 * Adapts a view's search request to the use case.
 */
public final class SearchRecipesController {
    private final SearchRecipesInputBoundary interactor;

    /**
     * Creates a controller for the search use case.
     *
     * @param interactor the search input boundary
     * @throws NullPointerException if interactor is null
     */
    public SearchRecipesController(SearchRecipesInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    /**
     * Passes a search request to the use case for validation and execution.
     *
     * @param keyword the user-entered search text; may be null
     * @param type the selected search mode; may be null
     */
    public void execute(String keyword, RecipeSearchType type) {
        interactor.execute(new SearchRecipesInputData(keyword, type));
    }
}
