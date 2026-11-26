package view;

import interface_adapter.ViewManagerModel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Shows one page at a time with a CardLayout, as in ca-login.
 *
 * <p>One addition (README rule 3): every page is registered together with an
 * "on shown" action, and the ViewManager runs that action each time the page
 * becomes visible, no matter who switched to it. Pages pass their own method,
 * for example {@code addView(name, page, page::onShown)}, so no page has to
 * change or implement a shared interface.</p>
 */
public class ViewManager implements PropertyChangeListener {

    private final JPanel cardPanel;
    private final CardLayout cardLayout;
    private final Map<String, Runnable> onShownActions = new HashMap<>();

    /**
     * Creates the view manager and starts listening to the model.
     *
     * @param cardPanel the panel that holds every page; it uses cardLayout
     * @param cardLayout the layout that shows one page at a time
     * @param viewManagerModel holds the name of the page to show
     */
    public ViewManager(JPanel cardPanel, CardLayout cardLayout,
                       ViewManagerModel viewManagerModel) {
        this.cardPanel = cardPanel;
        this.cardLayout = cardLayout;
        viewManagerModel.addPropertyChangeListener(this);
    }

    /**
     * Adds a page.
     *
     * @param viewName the name used to switch to this page
     * @param page the page itself
     * @param onShown what to run every time the page becomes visible
     */
    public void addView(String viewName, JComponent page, Runnable onShown) {
        cardPanel.add(page, viewName);
        onShownActions.put(viewName, onShown);
    }

    /**
     * Shows the page named in the event, then runs its "on shown" action.
     *
     * @param event the change fired by ViewManagerModel
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!ViewManagerModel.STATE_PROPERTY.equals(event.getPropertyName())) {
            return;
        }
        final String viewName = (String) event.getNewValue();
        cardLayout.show(cardPanel, viewName);
        final Runnable onShown = onShownActions.get(viewName);
        if (onShown != null) {
            onShown.run();
        }
    }
}
