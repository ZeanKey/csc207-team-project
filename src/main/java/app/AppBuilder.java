package app;

import data_access.FileMealPlanDataAccessObject;
import data_access.FilePantryDataAccessObject;
import data_access.FileShoppingListDataAccessObject;
import interface_adapter.ViewManagerModel;
import interface_adapter.generate_shopping_list.GenerateShoppingListController;
import interface_adapter.generate_shopping_list.GenerateShoppingListPresenter;
import interface_adapter.load_meal_plan.LoadMealPlanController;
import interface_adapter.load_meal_plan.LoadMealPlanPresenter;
import interface_adapter.load_pantry.LoadPantryController;
import interface_adapter.load_pantry.LoadPantryPresenter;
import interface_adapter.load_shopping_list.LoadShoppingListController;
import interface_adapter.load_shopping_list.LoadShoppingListPresenter;
import interface_adapter.meal_plan.MealPlanViewModel;
import interface_adapter.move_planned_meal.MovePlannedMealController;
import interface_adapter.move_planned_meal.MovePlannedMealPresenter;
import interface_adapter.pantry.PantryViewModel;
import interface_adapter.remove_planned_meal.RemovePlannedMealController;
import interface_adapter.remove_planned_meal.RemovePlannedMealPresenter;
import interface_adapter.shopping_list.ShoppingListViewModel;
import interface_adapter.toggle_bought.ToggleBoughtController;
import interface_adapter.toggle_bought.ToggleBoughtPresenter;
import use_case.generate_shopping_list.DefaultShoppingNeedPolicy;
import use_case.generate_shopping_list.GenerateShoppingListInteractor;
import use_case.load_meal_plan.LoadMealPlanInteractor;
import use_case.load_pantry.LoadPantryInteractor;
import use_case.load_shopping_list.LoadShoppingListInteractor;
import use_case.move_planned_meal.MovePlannedMealInteractor;
import use_case.remove_planned_meal.RemovePlannedMealInteractor;
import use_case.toggle_bought.ToggleBoughtInteractor;
import view.MealPlanView;
import view.NavigationBar;
import view.PantryView;
import view.ShoppingListView;
import view.ViewManager;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Builds the main MealMate window step by step, like ca-login's AppBuilder.
 *
 * <p>Each {@code addXxx} method creates one page, wires it to its
 * controllers, presenters and data access objects, registers it with the
 * ViewManager and adds its button to the navigation bar. Pages that are not
 * on main yet (Recipes, Cook) are placeholders; their owner replaces the
 * placeholder method with the real wiring.</p>
 */
public class AppBuilder {

    private static final String RECIPES_VIEW_NAME = "recipes";
    private static final String FRIDGE_VIEW_NAME = "fridge";
    private static final String COOK_VIEW_NAME = "cook";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final ViewManagerModel viewManagerModel = new ViewManagerModel();
    private final ViewManager viewManager =
            new ViewManager(cardPanel, cardLayout, viewManagerModel);
    private final NavigationBar navigationBar =
            new NavigationBar(viewManagerModel);

    private final FileMealPlanDataAccessObject mealPlanDataAccess;
    private final FilePantryDataAccessObject pantryDataAccess;
    private final FileShoppingListDataAccessObject shoppingListDataAccess;

    private String firstViewName;

    /**
     * Creates a builder whose pages read and save their files in the given
     * folder: {@code mealplans.json}, {@code pantry.json} and
     * {@code shopping_list.json}.
     *
     * @param dataFolder the folder for the data files, usually {@code data}
     */
    public AppBuilder(Path dataFolder) {
        mealPlanDataAccess = new FileMealPlanDataAccessObject(
                dataFolder.resolve("mealplans.json"));
        pantryDataAccess = new FilePantryDataAccessObject(
                dataFolder.resolve("pantry.json"));
        shoppingListDataAccess = new FileShoppingListDataAccessObject(
                dataFolder.resolve("shopping_list.json"));
    }

    /**
     * Adds a placeholder for the Recipes page (Use Case 1, Zean).
     *
     * @return this builder
     */
    public AppBuilder addRecipesPlaceholder() {
        return addPlaceholder("Recipes", RECIPES_VIEW_NAME,
                "Recipes: Zean adds RecipeSearchView here (Use Case 1).");
    }

    /**
     * Adds the Fridge page (Use Case 2). Uses the same
     * {@link FilePantryDataAccessObject} as the shopping list.
     *
     * @return this builder
     */
    public AppBuilder addFridgeView() {
        final PantryViewModel viewModel = new PantryViewModel();
        final LoadPantryController loadController =
                new LoadPantryController(new LoadPantryInteractor(
                        pantryDataAccess,
                        new LoadPantryPresenter(viewModel),
                        Clock.systemDefaultZone()));
        final PantryView view = new PantryView(viewModel, loadController);
        return addPage("Fridge", FRIDGE_VIEW_NAME, view, view::onShown);
    }

    /**
     * Adds the Meal Plan page (Use Case 3). Same wiring as MealPlanDemo, but
     * the meal plans come from the file instead of memory.
     *
     * @return this builder
     */
    public AppBuilder addMealPlanView() {
        final MealPlanViewModel viewModel = new MealPlanViewModel();
        final LoadMealPlanController loadController =
                new LoadMealPlanController(new LoadMealPlanInteractor(
                        mealPlanDataAccess,
                        new LoadMealPlanPresenter(viewModel)));
        final RemovePlannedMealController removeController =
                new RemovePlannedMealController(new RemovePlannedMealInteractor(
                        mealPlanDataAccess,
                        new RemovePlannedMealPresenter(viewModel)));
        final MovePlannedMealController moveController =
                new MovePlannedMealController(new MovePlannedMealInteractor(
                        mealPlanDataAccess,
                        new MovePlannedMealPresenter(viewModel)));
        final MealPlanView view = new MealPlanView(
                viewModel, loadController, removeController, moveController);
        return addPage("Meal Plan", MealPlanViewModel.VIEW_NAME,
                view, view::onShown);
    }

    /**
     * Adds the Shopping List page (Use Case 4). Same wiring as
     * ShoppingListDemo, without the sample data.
     *
     * @return this builder
     */
    public AppBuilder addShoppingListView() {
        final ShoppingListViewModel viewModel = new ShoppingListViewModel();
        final LoadShoppingListController loadController =
                new LoadShoppingListController(new LoadShoppingListInteractor(
                        shoppingListDataAccess,
                        new LoadShoppingListPresenter(viewModel)));
        final GenerateShoppingListController generateController =
                new GenerateShoppingListController(
                        new GenerateShoppingListInteractor(
                                mealPlanDataAccess,
                                pantryDataAccess,
                                shoppingListDataAccess,
                                new DefaultShoppingNeedPolicy(),
                                new GenerateShoppingListPresenter(viewModel),
                                Clock.systemDefaultZone()));
        final ToggleBoughtController toggleController =
                new ToggleBoughtController(new ToggleBoughtInteractor(
                        shoppingListDataAccess,
                        new ToggleBoughtPresenter(viewModel)));
        final ShoppingListView view = new ShoppingListView(viewModel,
                loadController, generateController, toggleController);
        return addPage("Shopping List", ShoppingListViewModel.VIEW_NAME,
                view, view::onShown);
    }

    /**
     * Adds a placeholder for the Cook page (Use Case 5, Jesse).
     *
     * @return this builder
     */
    public AppBuilder addCookPlaceholder() {
        return addPlaceholder("Cook", COOK_VIEW_NAME,
                "Cook: Jesse adds CookView here (Use Case 5).");
    }

    /**
     * Puts the navigation bar on top and the pages below it, and shows the
     * first page that was added.
     *
     * @return the panel holding the whole app
     */
    public JPanel buildMainPanel() {
        final JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(navigationBar, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);
        if (firstViewName != null) {
            viewManagerModel.setState(firstViewName);
            viewManagerModel.firePropertyChanged();
        }
        return mainPanel;
    }

    /**
     * Creates the main window around {@link #buildMainPanel()}.
     *
     * @return the main window, not visible yet
     */
    public JFrame build() {
        final JFrame application = new JFrame("MealMate");
        application.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        application.setContentPane(buildMainPanel());
        return application;
    }

    private AppBuilder addPlaceholder(String label, String viewName,
                                      String text) {
        final JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.add(new JLabel(text, SwingConstants.CENTER),
                BorderLayout.CENTER);
        return addPage(label, viewName, placeholder, () -> { });
    }

    private AppBuilder addPage(String label, String viewName,
                               JComponent page, Runnable onShown) {
        viewManager.addView(viewName, page, onShown);
        navigationBar.addButton(label, viewName);
        if (firstViewName == null) {
            firstViewName = viewName;
        }
        return this;
    }
}
