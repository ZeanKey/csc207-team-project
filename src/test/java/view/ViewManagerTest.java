package view;

import interface_adapter.ViewManagerModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.CardLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that ViewManager switches pages and calls "on shown" every time
 * (README "View Architecture", rule 3).
 */
class ViewManagerTest {

    private final ViewManagerModel viewManagerModel = new ViewManagerModel();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JPanel firstPage = new JPanel();
    private final JPanel secondPage = new JPanel();
    private int secondPageShownCount;

    @BeforeEach
    void setUp() {
        final ViewManager viewManager =
                new ViewManager(cardPanel, cardLayout, viewManagerModel);
        viewManager.addView("first", firstPage, () -> { });
        viewManager.addView("second", secondPage, () -> secondPageShownCount++);
    }

    @Test
    void showsTheRequestedPage() {
        show("second");

        assertTrue(secondPage.isVisible());
        assertFalse(firstPage.isVisible());
        assertEquals("second", viewManagerModel.getState());
    }

    @Test
    void runsOnShownEveryTimeThePageBecomesVisible() {
        show("second");
        show("first");
        show("second");

        assertEquals(2, secondPageShownCount);
    }

    private void show(String viewName) {
        viewManagerModel.setState(viewName);
        viewManagerModel.firePropertyChanged();
    }
}
