package use_case.search_recipes;

import entity.Recipe;
import interface_adapter.search_recipes.SearchRecipesPresenter;
import interface_adapter.search_recipes.SearchRecipesViewModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRecipesPresenterTest {
    @Test
    void presentsResultsAndEmptyState() {
        SearchRecipesViewModel model = new SearchRecipesViewModel();
        SearchRecipesPresenter presenter = new SearchRecipesPresenter(model);
        AtomicBoolean notified = new AtomicBoolean();
        model.addListener(() -> notified.set(true));
        Recipe recipe = new Recipe("meal:1", "Soup", null, null, null, null, List.of());

        presenter.prepareSuccessView(new SearchRecipesOutputData(List.of(recipe)));
        assertEquals(List.of(recipe), model.getRecipes());
        assertEquals("", model.getMessage());
        assertTrue(notified.get());

        presenter.prepareSuccessView(new SearchRecipesOutputData(List.of()));
        assertTrue(model.getRecipes().isEmpty());
        assertEquals("No recipes found.", model.getMessage());
    }

    @Test
    void failureClearsPreviousResultsAndPresentsError() {
        SearchRecipesViewModel model = new SearchRecipesViewModel();
        SearchRecipesPresenter presenter = new SearchRecipesPresenter(model);
        Recipe recipe = new Recipe("meal:1", "Soup", null, null, null, null, List.of());
        presenter.prepareSuccessView(new SearchRecipesOutputData(List.of(recipe)));
        presenter.prepareFailView("Please try again.");
        assertTrue(model.getRecipes().isEmpty());
        assertEquals("Please try again.", model.getMessage());
    }
}
