package interface_adapter.search_recipes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ViewModel for the Recipes page (use case 1a).
 *
 * <p>Searches run off the Swing thread, so listeners may be notified from a
 * background thread; views are responsible for moving back to the Swing
 * thread before touching components.</p>
 */
public class SearchRecipesViewModel {

    public static final String VIEW_NAME = "recipes";

    private final List<SearchRecipesViewModelListener> listeners =
            new CopyOnWriteArrayList<>();
    private volatile SearchRecipesState state = new SearchRecipesState();

    /**
     * Returns the current state.
     *
     * @return the state
     */
    public SearchRecipesState getState() {
        return state;
    }

    /**
     * Replaces the state without notifying listeners.
     *
     * @param state the new state
     */
    public void setState(SearchRecipesState state) {
        this.state = state;
    }

    /**
     * Registers a listener for state changes.
     *
     * @param listener the listener
     */
    public void addListener(SearchRecipesViewModelListener listener) {
        listeners.add(listener);
    }

    /**
     * Notifies all listeners that the state has changed.
     */
    public void notifyStateChanged() {
        for (SearchRecipesViewModelListener listener : listeners) {
            listener.stateChanged();
        }
    }
}
