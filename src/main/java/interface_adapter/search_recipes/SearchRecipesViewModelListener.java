package interface_adapter.search_recipes;

/**
 * Listener notified when the Recipes page state changes.
 */
public interface SearchRecipesViewModelListener {

    /**
     * The search state was updated. This may be called on any thread.
     */
    void stateChanged();
}
