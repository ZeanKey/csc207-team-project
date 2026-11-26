package use_case.search_recipes;

import entity.Recipe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates a search and presents a summary of each matching recipe.
 */
public final class SearchRecipesInteractor implements SearchRecipesInputBoundary {
    private final SearchRecipesDataAccessInterface dataAccess;
    private final SearchRecipesOutputBoundary presenter;

    /**
     * Creates a search interactor.
     *
     * @param dataAccess the recipe search data source
     * @param presenter the output boundary receiving search results
     * @throws NullPointerException if either dependency is null
     */
    public SearchRecipesInteractor(SearchRecipesDataAccessInterface dataAccess,
                                   SearchRecipesOutputBoundary presenter) {
        this.dataAccess = Objects.requireNonNull(dataAccess);
        this.presenter = Objects.requireNonNull(presenter);
    }

    /**
     * Searches using the provided input and presents either results or an error.
     * Invalid input and data access failures are presented as errors.
     *
     * @param inputData the search request, or null for an invalid request
     * @throws NullPointerException if the data source returns a null list or a null recipe
     */
    @Override
    public void execute(SearchRecipesInputData inputData) {
        if (inputData == null || inputData.getKeyword() == null || inputData.getKeyword().isBlank()) {
            presenter.prepareFailView("Please enter a search keyword.");
            return;
        }
        if (inputData.getSearchType() == null) {
            presenter.prepareFailView("Please choose a search type.");
            return;
        }
        String keyword = inputData.getKeyword().strip();
        final List<Recipe> recipes;
        try {
            recipes = inputData.getSearchType() == RecipeSearchType.NAME
                    ? dataAccess.searchByName(keyword) : dataAccess.searchByIngredient(keyword);
        }
        catch (IOException exception) {
            presenter.prepareFailView("The recipe service could not complete the search. Please try again.");
            return;
        }
        final List<RecipeSummaryOutputData> results = new ArrayList<>();
        for (Recipe recipe : recipes) {
            results.add(new RecipeSummaryOutputData(
                    recipe.getId(), recipe.getName(), recipe.getThumbnail()));
        }
        presenter.prepareSuccessView(new SearchRecipesOutputData(results));
    }
}
