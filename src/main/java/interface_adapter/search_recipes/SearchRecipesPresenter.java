package interface_adapter.search_recipes;

import use_case.search_recipes.SearchRecipesOutputBoundary;
import use_case.search_recipes.SearchRecipesOutputData;

import java.util.List;
import java.util.Objects;

/** Converts search results to view state. */
public final class SearchRecipesPresenter implements SearchRecipesOutputBoundary {
    private final SearchRecipesViewModel viewModel;

    public SearchRecipesPresenter(SearchRecipesViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
    }

    @Override
    public void prepareSuccessView(SearchRecipesOutputData outputData) {
        String message = outputData.getRecipes().isEmpty() ? "No recipes found." : "";
        viewModel.setResults(outputData.getRecipes(), message);
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.setResults(List.of(), message);
    }
}
