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

    public SearchRecipesController(SearchRecipesInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    public void execute(String keyword, RecipeSearchType type) {
        interactor.execute(new SearchRecipesInputData(keyword, type));
    }
}
