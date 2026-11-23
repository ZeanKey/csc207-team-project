package use_case.search_recipes;

import entity.Recipe;
import java.util.List;
import java.util.Objects;

/** Validates a search and presents complete matching recipes. */
public final class SearchRecipesInteractor implements SearchRecipesInputBoundary {
    private final SearchRecipesDataAccessInterface dataAccess;
    private final SearchRecipesOutputBoundary presenter;

    public SearchRecipesInteractor(SearchRecipesDataAccessInterface dataAccess,
                                   SearchRecipesOutputBoundary presenter) {
        this.dataAccess = Objects.requireNonNull(dataAccess);
        this.presenter = Objects.requireNonNull(presenter);
    }

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
        try {
            List<Recipe> recipes = inputData.getSearchType() == RecipeSearchType.NAME
                    ? dataAccess.searchByName(keyword) : dataAccess.searchByIngredient(keyword);
            presenter.prepareSuccessView(new SearchRecipesOutputData(recipes));
        }
        catch (SearchRecipesDataAccessException exception) {
            presenter.prepareFailView("The recipe service could not complete the search. Please try again.");
        }
    }
}
