package interface_adapter.search_recipes;

import use_case.search_recipes.RecipeSummaryOutputData;
import use_case.search_recipes.SearchRecipesOutputBoundary;
import use_case.search_recipes.SearchRecipesOutputData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Presenter for the Search Recipes use case.
 */
public final class SearchRecipesPresenter implements SearchRecipesOutputBoundary {
    static final String NO_RESULTS_MESSAGE = "No recipes found.";

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
     * Shows one row per matching recipe, or a no-results message.
     *
     * @param outputData the successful search results
     */
    @Override
    public void prepareSuccessView(SearchRecipesOutputData outputData) {
        final List<RecipeSearchResultViewData> rows = new ArrayList<>();
        for (RecipeSummaryOutputData result : outputData.getResults()) {
            final String thumbnail = result.getThumbnail() == null
                    ? "" : result.getThumbnail().toString();
            rows.add(new RecipeSearchResultViewData(
                    result.getId(), result.getName(), thumbnail));
        }
        final SearchRecipesState state = new SearchRecipesState();
        state.setResults(rows);
        state.setMessage(rows.isEmpty() ? NO_RESULTS_MESSAGE : "");
        viewModel.setState(state);
        viewModel.notifyStateChanged();
    }

    /**
     * Clears previous results and shows the error message.
     *
     * @param message the error message to display
     */
    @Override
    public void prepareFailView(String message) {
        final SearchRecipesState state = new SearchRecipesState();
        state.setMessage(message);
        viewModel.setState(state);
        viewModel.notifyStateChanged();
    }
}
