package app;

import data_access.FileCookingLogDataAccessObject;
import data_access.FileMealPlanDataAccessObject;
import data_access.FileShoppingListDataAccessObject;
import entity.CookingRecord;
import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.MealType;
import entity.PlannedMeal;
import entity.ShoppingItem;
import entity.ShoppingList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import view.MealPlanView;
import view.ShoppingListView;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
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
import java.util.function.Predicate;

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
                .addCookView()
                .buildMainPanel();

        assertFalse(visiblePage(mainPanel) instanceof ShoppingListView);

        navButton(mainPanel, "Shopping List").doClick();
        final Component shoppingPage = visiblePage(mainPanel);
        assertInstanceOf(ShoppingListView.class, shoppingPage);
        assertNotNull(findCheckBox((Container) shoppingPage, "Garlic"));

        navButton(mainPanel, "Meal Plan").doClick();
        assertInstanceOf(MealPlanView.class, visiblePage(mainPanel));
    }

    @Test
    void cookAndHistoryReadTheirFilesThroughAppNavigation() {
        final LocalDate today = LocalDate.now();
        final LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final MealSlot dinner = new MealSlot(DayOfWeek.MONDAY, MealType.DINNER);
        final MealPlan plan = new MealPlan(monday);
        plan.assign(dinner, new PlannedMeal("pasta", "Pasta", List.of()));
        new FileMealPlanDataAccessObject(dataFolder.resolve("mealplans.json"))
                .saveMealPlan(plan);

        final JPanel mainPanel = new AppBuilder(dataFolder)
                .addMealPlanView().addCookView().buildMainPanel();
        navButton(mainPanel, "Cook").doClick();
        final CookingScreens cook = assertInstanceOf(CookingScreens.class, visiblePage(mainPanel));
        assertTrue(visibleListText(cook).contains("Pasta"));

        new FileCookingLogDataAccessObject(dataFolder.resolve("cooking_log.json"))
                .append(new CookingRecord("soup", "Soup", today, monday,
                        new MealSlot(DayOfWeek.MONDAY, MealType.LUNCH)));
        final JButton historyButton = assertInstanceOf(JButton.class,
                findVisible(cook, child -> child instanceof JButton button
                        && "History".equals(button.getText())));
        historyButton.doClick();
        assertTrue(visibleListText(cook).contains("Soup"));

        navButton(mainPanel, "Meal Plan").doClick();
        navButton(mainPanel, "Cook").doClick();
        assertTrue(visibleListText(cook).contains("Pasta"));
    }

    private static String visibleListText(Container panel) {
        final JList<?> list = assertInstanceOf(JList.class,
                findVisible(panel, child -> child instanceof JList<?>));
        return list.getModel().getElementAt(0).toString();
    }

    private static Component findVisible(Container panel, Predicate<Component> matches) {
        for (Component child : panel.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            if (matches.test(child)) {
                return child;
            }
            if (child instanceof Container container) {
                final Component found = findVisible(container, matches);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
