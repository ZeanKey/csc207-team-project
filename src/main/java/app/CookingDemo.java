package app;

import data_access.FileCookingDataAccessObject;
import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.MealType;
import entity.Pantry;
import entity.PantryItem;
import entity.PlannedMeal;
import entity.RecipeIngredient;
import entity.StockLevel;
import interface_adapter.cooking.CookingController;
import interface_adapter.cooking.CookingPresenter;
import interface_adapter.cooking.CookingViewModel;
import use_case.complete_meal.CompleteMealInteractor;
import use_case.load_cooking_history.LoadCookingHistoryInteractor;
import use_case.load_uncooked_meals.LoadUncookedMealsInteractor;
import use_case.prepare_checklist.PrepareChecklistInteractor;
import view.CookView;
import view.CookingHistoryView;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Runs the Cook and History screens using sample meals and a local demo file. */
public final class CookingDemo {
    private static final String COOK = "cook";
    private static final String HISTORY = "history";
    private static final Path DEFAULT_FILE = Path.of("data", "cooking-demo.json");

    private CookingDemo() {
    }

    public static void main(String[] args) {
        final Path file = args.length == 0 ? DEFAULT_FILE : Path.of(args[0]);
        SwingUtilities.invokeLater(() -> showFrame(file));
    }

    private static void showFrame(Path file) {
        final JPanel screens;
        try {
            screens = createScreens(file, Clock.systemDefaultZone());
        }
        catch (IllegalStateException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), "Cooking", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final JFrame frame = new JFrame("MealMate — Cooking Demo");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(screens);
        frame.setSize(520, 360);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static JPanel createScreens(Path file, Clock clock) {
        final LocalDate week = LocalDate.now(clock)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final FileCookingDataAccessObject store =
                new FileCookingDataAccessObject(file, samplePlan(week), samplePantry());
        final CookingViewModel viewModel = new CookingViewModel();
        final CookingPresenter presenter = new CookingPresenter(viewModel);
        final CookingController controller = new CookingController(
                new LoadUncookedMealsInteractor(store, presenter, clock),
                new PrepareChecklistInteractor(store, presenter),
                new CompleteMealInteractor(store, presenter, clock),
                new LoadCookingHistoryInteractor(store, presenter));

        final CardLayout cards = new CardLayout();
        final JPanel screens = new JPanel(cards);
        final CookingHistoryView historyView = new CookingHistoryView(viewModel, controller, () -> {
            cards.show(screens, COOK);
            controller.loadMeals();
        });
        final CookView cookView = new CookView(viewModel, controller, () -> {
            cards.show(screens, HISTORY);
            historyView.onShown();
        });
        screens.add(cookView, COOK);
        screens.add(historyView, HISTORY);
        cookView.onShown();
        return screens;
    }

    private static MealPlan samplePlan(LocalDate week) {
        final MealPlan plan = new MealPlan(week);
        plan.assign(new MealSlot(DayOfWeek.WEDNESDAY, MealType.DINNER),
                new PlannedMeal("pasta", "Tomato pasta", List.of(
                        ingredient("Garlic", "2 cloves"),
                        ingredient("Pasta", "200 g"),
                        ingredient("Tomato", "2"))));
        plan.assign(new MealSlot(DayOfWeek.SATURDAY, MealType.LUNCH),
                new PlannedMeal("soup", "Carrot soup", List.of(
                        ingredient("Garlic", "1 clove"),
                        ingredient("Carrot", "3"))));
        return plan;
    }

    private static Pantry samplePantry() {
        final Pantry pantry = new Pantry();
        pantry.add(new PantryItem(IngredientName.fromRawName("Garlic"), null, StockLevel.ENOUGH));
        pantry.add(new PantryItem(IngredientName.fromRawName("Pasta"), null, StockLevel.LOW));
        pantry.add(new PantryItem(IngredientName.fromRawName("Carrot"), null, StockLevel.ENOUGH));
        return pantry;
    }

    private static RecipeIngredient ingredient(String name, String measure) {
        return new RecipeIngredient(IngredientName.fromRawName(name), measure);
    }
}
