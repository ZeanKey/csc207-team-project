package app;

import data_access.FileShoppingListDataAccessObject;
import entity.IngredientName;
import entity.ShoppingItem;
import entity.ShoppingList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import view.MealPlanView;
import view.ShoppingListView;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the main panel through its navigation buttons, with data files in a
 * temporary folder.
 */
class AppBuilderTest {

    @TempDir
    Path dataFolder;

    @Test
    void navButtonsSwitchPagesAndReloadThemFromFile() {
        final LocalDate monday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        new FileShoppingListDataAccessObject(
                dataFolder.resolve("shopping_list.json"))
                .saveShoppingList(new ShoppingList(monday, List.of(
                        new ShoppingItem(IngredientName.fromRawName("Garlic"),
                                ShoppingItem.Reason.MISSING, Set.of("Pasta")))));

        final JPanel mainPanel = new AppBuilder(dataFolder)
                .addRecipesPlaceholder()
                .addFridgePlaceholder()
                .addMealPlanView()
                .addShoppingListView()
                .addCookPlaceholder()
                .buildMainPanel();

        assertFalse(visiblePage(mainPanel) instanceof ShoppingListView);

        navButton(mainPanel, "Shopping List").doClick();
        final Component shoppingPage = visiblePage(mainPanel);
        assertInstanceOf(ShoppingListView.class, shoppingPage);
        assertNotNull(findCheckBox((Container) shoppingPage, "Garlic"));

        navButton(mainPanel, "Meal Plan").doClick();
        assertInstanceOf(MealPlanView.class, visiblePage(mainPanel));
    }

    private static Component part(JPanel mainPanel, String position) {
        return ((BorderLayout) mainPanel.getLayout())
                .getLayoutComponent(position);
    }

    private static JButton navButton(JPanel mainPanel, String label) {
        final Container navigationBar =
                (Container) part(mainPanel, BorderLayout.NORTH);
        for (Component component : navigationBar.getComponents()) {
            if (component instanceof JButton
                    && label.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
        }
        throw new AssertionError("No nav button called " + label);
    }

    private static Component visiblePage(JPanel mainPanel) {
        final Container cardPanel =
                (Container) part(mainPanel, BorderLayout.CENTER);
        for (Component page : cardPanel.getComponents()) {
            if (page.isVisible()) {
                return page;
            }
        }
        throw new AssertionError("No page is visible");
    }

    private static JCheckBox findCheckBox(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JCheckBox
                    && text.equals(((JCheckBox) component).getText())) {
                return (JCheckBox) component;
            }
            if (component instanceof Container) {
                final JCheckBox found =
                        findCheckBox((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
