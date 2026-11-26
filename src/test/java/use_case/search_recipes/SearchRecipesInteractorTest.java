package use_case.search_recipes;

import entity.Recipe;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRecipesInteractorTest {
    private final FakeDao dao = new FakeDao();
    private final FakePresenter presenter = new FakePresenter();
    private final SearchRecipesInteractor interactor = new SearchRecipesInteractor(dao, presenter);

    @Test
    void searchesByNameAndPresentsASummaryOfEachRecipe() {
        URI picture = URI.create("https://img/soup.jpg");
        dao.results = List.of(
                new Recipe("meal:1", "Soup", "Starter", "French", "Boil.", picture, List.of()),
                new Recipe("meal:2", "Bread", null, null, null, null, List.of()));

        interactor.execute(new SearchRecipesInputData("  Soup  ", RecipeSearchType.NAME));

        assertEquals("name:Soup", dao.request);
        List<RecipeSummaryOutputData> results = presenter.output.getResults();
        assertEquals(2, results.size());
        assertEquals("meal:1", results.get(0).getId());
        assertEquals("Soup", results.get(0).getName());
        assertEquals(picture, results.get(0).getThumbnail());
        assertEquals("meal:2", results.get(1).getId());
        assertNull(results.get(1).getThumbnail());
        assertNull(presenter.error);
    }

    @Test
    void searchesByIngredientAndReportsEmptyResultsAsSuccess() {
        interactor.execute(new SearchRecipesInputData(" garlic ", RecipeSearchType.INGREDIENT));

        assertEquals("ingredient:garlic", dao.request);
        assertTrue(presenter.output.getResults().isEmpty());
        assertNull(presenter.error);
    }

    @Test
    void rejectsBlankInputAndMissingSearchTypeWithoutCallingDao() {
        interactor.execute(new SearchRecipesInputData("  ", RecipeSearchType.NAME));
        assertEquals("Please enter a search keyword.", presenter.error);
        assertNull(dao.request);

        interactor.execute(new SearchRecipesInputData("soup", null));
        assertEquals("Please choose a search type.", presenter.error);
        assertNull(dao.request);
    }

    @Test
    void rejectsMissingRequest() {
        interactor.execute(null);

        assertEquals("Please enter a search keyword.", presenter.error);
        assertNull(dao.request);
    }

    @Test
    void reportsIngredientSearchFailure() {
        dao.fail = true;
        interactor.execute(new SearchRecipesInputData("garlic", RecipeSearchType.INGREDIENT));

        assertNull(presenter.output);
        assertTrue(presenter.error.contains("try again"));
    }

    @Test
    void reportsServiceFailureInsteadOfEmptyResults() {
        dao.fail = true;
        interactor.execute(new SearchRecipesInputData("soup", RecipeSearchType.NAME));

        assertNull(presenter.output);
        assertTrue(presenter.error.contains("try again"));
    }

    private static final class FakeDao implements SearchRecipesDataAccessInterface {
        private String request;
        private boolean fail;
        private List<Recipe> results = List.of();

        @Override
        public List<Recipe> searchByName(String keyword) throws IOException {
            request = "name:" + keyword;
            if (fail) {
                throw new IOException("unavailable");
            }
            return results;
        }

        @Override
        public List<Recipe> searchByIngredient(String keyword) throws IOException {
            request = "ingredient:" + keyword;
            if (fail) {
                throw new IOException("unavailable");
            }
            return results;
        }
    }

    private static final class FakePresenter implements SearchRecipesOutputBoundary {
        private SearchRecipesOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(SearchRecipesOutputData outputData) {
            output = outputData;
        }

        @Override
        public void prepareFailView(String message) {
            error = message;
        }
    }
}
