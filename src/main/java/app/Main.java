package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Starts the full MealMate app: one window with the navigation bar and all
 * five pages. Data is saved in the {@code data} folder.
 */
public final class Main {

    /** Width of the main window, in pixels. */
    private static final int WINDOW_WIDTH = 1000;
    /** Height of the main window, in pixels. */
    private static final int WINDOW_HEIGHT = 600;

    private Main() {
        // Only main is used, so no instances are needed.
    }

    /**
     * Opens the main window.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShow);
    }

    private static void createAndShow() {
        final JFrame application = new AppBuilder(Path.of("data"))
                .addRecipesPlaceholder()
                .addFridgeView()
                .addMealPlanView()
                .addShoppingListView()
                .addCookView()
                .build();
        application.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        application.setLocationRelativeTo(null);
        application.setVisible(true);
    }
}
