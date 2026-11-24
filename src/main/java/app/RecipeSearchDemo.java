package app;

import data_access.MealDbRecipeDataAccessObject;
import interface_adapter.search_recipes.SearchRecipesController;
import interface_adapter.search_recipes.SearchRecipesPresenter;
import interface_adapter.search_recipes.SearchRecipesViewModel;
import use_case.search_recipes.SearchRecipesInteractor;
import view.RecipeSearchView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Standalone entry point until the team's use cases are assembled together.
 */
public final class RecipeSearchDemo {
    private RecipeSearchDemo() { }

    /**
     * Starts the standalone search window.
     *
     * @param args unused command-line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SearchRecipesViewModel viewModel = new SearchRecipesViewModel();
            SearchRecipesController controller = new SearchRecipesController(
                    new SearchRecipesInteractor(new MealDbRecipeDataAccessObject(),
                            new SearchRecipesPresenter(viewModel)));
            JFrame frame = new JFrame("MealMate — Search Recipes");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(new RecipeSearchView(viewModel, controller));
            frame.setSize(700, 500);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
