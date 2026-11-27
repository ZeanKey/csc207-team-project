package interface_adapter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Remembers which page is visible, by its view name (README "View
 * Architecture").
 *
 * <p>Same idea as ca-login's ViewManagerModel: whoever wants to switch pages
 * (the navigation bar, or a Presenter such as the one behind a "Back" button)
 * calls {@link #setState(String)} and then {@link #firePropertyChanged()}.
 * The ViewManager listens and shows that page.</p>
 */
public class ViewManagerModel {

    /** Name of the property that listeners receive when the page changes. */
    public static final String STATE_PROPERTY = "state";

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);
    private String state = "";

    /**
     * Returns the name of the page that should be visible.
     *
     * @return the view name, or an empty string before any page is shown
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the name of the page that should be visible. Listeners are not
     * told until {@link #firePropertyChanged()} is called.
     *
     * @param viewName the view name of the page to show
     */
    public void setState(String viewName) {
        this.state = viewName;
    }

    /**
     * Tells every listener which page should now be visible.
     */
    public void firePropertyChanged() {
        support.firePropertyChange(STATE_PROPERTY, null, state);
    }

    /**
     * Registers a listener, usually the ViewManager.
     *
     * @param listener the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
