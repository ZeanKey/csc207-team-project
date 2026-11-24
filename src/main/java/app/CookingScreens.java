package app;

import data_access.SharedCookingDataAccessObject;
import data_access.SharedCookingDataAccessObject.HistoryStore;
import interface_adapter.cooking.CookingController;
import interface_adapter.cooking.CookingPresenter;
import interface_adapter.cooking.CookingViewModel;
import use_case.complete_meal.CompleteMealDataAccessInterface;
import use_case.complete_meal.CompleteMealInteractor;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary;
import use_case.load_cooking_history.LoadCookingHistoryInteractor;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary;
import use_case.load_uncooked_meals.LoadUncookedMealsInteractor;
import use_case.move_planned_meal.MovePlannedMealDataAccessInterface;
import use_case.prepare_checklist.PrepareChecklistInputBoundary;
import use_case.prepare_checklist.PrepareChecklistInteractor;
import use_case.update_stock_level.UpdateStockLevelDataAccessInterface;
import view.CookView;
import view.CookingHistoryView;

import javax.swing.JPanel;
import java.awt.CardLayout;
import java.time.Clock;

/** Builds the Cook and History screens for the demo or shared Swing application. */
public final class CookingScreens extends JPanel {
    private static final String COOK = "cook";
    private static final String HISTORY = "history";
    private final CardLayout cards = new CardLayout();
    private final CookView cookView;

    /** Pass the application's existing plan and pantry stores when wiring Cooking. */
    public CookingScreens(MovePlannedMealDataAccessInterface mealPlans,
                          UpdateStockLevelDataAccessInterface pantry, HistoryStore history, Clock clock) {
        this(new SharedCookingDataAccessObject(mealPlans, pantry, history), clock);
    }

    private CookingScreens(SharedCookingDataAccessObject store, Clock clock) {
        this(store, store, store, store, clock);
    }

    CookingScreens(LoadUncookedMealsInputBoundary.DataAccess meals,
                   PrepareChecklistInputBoundary.DataAccess checklist,
                   CompleteMealDataAccessInterface completion,
                   LoadCookingHistoryInputBoundary.DataAccess history, Clock clock) {
        final CookingViewModel model = new CookingViewModel();
        final CookingPresenter presenter = new CookingPresenter(model);
        final CookingController controller = new CookingController(
                new LoadUncookedMealsInteractor(meals, presenter, clock),
                new PrepareChecklistInteractor(checklist, presenter),
                new CompleteMealInteractor(completion, presenter, clock),
                new LoadCookingHistoryInteractor(history, presenter));

        setLayout(cards);
        final CookingHistoryView historyView = new CookingHistoryView(model, controller, this::onShown);
        cookView = new CookView(model, controller, () -> {
            cards.show(this, HISTORY);
            historyView.onShown();
        });
        add(cookView, COOK);
        add(historyView, HISTORY);
    }

    /** Reloads the pending list when the user opens Cooking. */
    public void onShown() {
        cards.show(this, COOK);
        cookView.onShown();
    }
}
