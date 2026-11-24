package view;

import interface_adapter.ViewManagerModel;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/**
 * The fixed row of page buttons at the top of the main window: Recipes,
 * Fridge, Meal Plan, Shopping List and Cook.
 *
 * <p>A button only changes which page is visible; no business rule is
 * involved, so it writes the page name to ViewManagerModel directly instead
 * of going through a use case.</p>
 */
public class NavigationBar extends JPanel {

    private final ViewManagerModel viewManagerModel;

    /**
     * Creates a navigation bar with no buttons yet.
     *
     * @param viewManagerModel the model that decides which page is visible
     */
    public NavigationBar(ViewManagerModel viewManagerModel) {
        super(new FlowLayout(FlowLayout.LEFT));
        this.viewManagerModel = viewManagerModel;
    }

    /**
     * Adds a button that switches to the given page.
     *
     * @param label the button text
     * @param viewName the page to show when the button is pressed
     */
    public void addButton(String label, String viewName) {
        final JButton button = new JButton(label);
        button.addActionListener(event -> {
            viewManagerModel.setState(viewName);
            viewManagerModel.firePropertyChanged();
        });
        add(button);
    }
}
