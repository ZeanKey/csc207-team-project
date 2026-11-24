package interface_adapter.search_recipes;

import use_case.search_recipes.SearchRecipesOutputBoundary;
import use_case.search_recipes.SearchRecipesOutputData;

import java.util.List;
import java.util.Objects;

/**
 * Converts search results to view state.
 */
public final class SearchRecipesPresenter implements SearchRecipesOutputBoundary {
    private final SearchRecipesViewModel viewModel;

    /**
     * Creates a presenter for the search view model.
     *
     * @param viewModel the model receiving search state
     * @throws NullPointerException if viewModel is null
     */
    public SearchRecipesPresenter(SearchRecipesViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
    }

    /**
     * Updates the view model with the results or a no-results message.
     *
     * @param outputData the successful search results
     * @throws NullPointerException if outputData is null
     */
    @Override
    public void prepareSuccessView(SearchRecipesOutputData outputData) {
        String message = outputData.getRecipes().isEmpty() ? "No recipes found." : "";
        viewModel.setResults(outputData.getRecipes(), message);
    }

    /**
     * Clears previous results and displays an error message.
     *
     * @param message the error message to display
     */
    @Override
    public void prepareFailView(String message) {
        viewModel.setResults(List.of(), message);
    }
}
