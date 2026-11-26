package interface_adapter.search_recipes;

import org.junit.jupiter.api.Test;
import use_case.search_recipes.RecipeSummaryOutputData;
import use_case.search_recipes.SearchRecipesOutputData;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRecipesPresenterTest {
    private final SearchRecipesViewModel viewModel = new SearchRecipesViewModel();
    private final SearchRecipesPresenter presenter = new SearchRecipesPresenter(viewModel);
    private final AtomicInteger notifications = new AtomicInteger();

    SearchRecipesPresenterTest() {
        viewModel.addListener(notifications::incrementAndGet);
    }

    @Test
    void successTurnsEachResultIntoARow() {
        presenter.prepareSuccessView(new SearchRecipesOutputData(List.of(
                new RecipeSummaryOutputData("meal:1", "Soup", URI.create("https://img/soup.jpg")),
                new RecipeSummaryOutputData("meal:2", "Bread", null))));

        final SearchRecipesState state = viewModel.getState();
        assertEquals(2, state.getResults().size());
        final RecipeSearchResultViewData soup = state.getResults().get(0);
        assertEquals("meal:1", soup.getId());
        assertEquals("Soup", soup.getName());
        assertEquals("https://img/soup.jpg", soup.getThumbnailUrl());
        assertEquals("", state.getResults().get(1).getThumbnailUrl());
        assertEquals("", state.getMessage());
        assertEquals(1, notifications.get());
    }

    @Test
    void emptySuccessShowsNoResultsMessage() {
        presenter.prepareSuccessView(new SearchRecipesOutputData(List.of()));

        assertTrue(viewModel.getState().getResults().isEmpty());
        assertEquals(SearchRecipesPresenter.NO_RESULTS_MESSAGE, viewModel.getState().getMessage());
        assertEquals(1, notifications.get());
    }

    @Test
    void failureClearsPreviousResultsAndShowsError() {
        presenter.prepareSuccessView(new SearchRecipesOutputData(List.of(
                new RecipeSummaryOutputData("meal:1", "Soup", null))));

        presenter.prepareFailView("Please try again.");

        assertTrue(viewModel.getState().getResults().isEmpty());
        assertEquals("Please try again.", viewModel.getState().getMessage());
        assertEquals(2, notifications.get());
    }
}
