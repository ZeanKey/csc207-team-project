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

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Runs the Cook and History screens using sample meals and a local demo file. */
public final class CookingDemo {
    private static final Path DEFAULT_FILE = Path.of("target", "demo-data", "cooking-demo.json");

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
        final CookingScreens screens = new CookingScreens(store, store, store, store, clock);
        screens.onShown();
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
