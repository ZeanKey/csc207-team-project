package interface_adapter.search_recipes;

import entity.Recipe;
import java.util.ArrayList;
import java.util.List;

/**
 * State observed by views displaying recipe search results.
 * Listeners run on the thread that updates the model.
 */
public final class SearchRecipesViewModel {
    private final List<Runnable> listeners = new ArrayList<>();
    private volatile List<Recipe> recipes = List.of();
    private volatile String message = "";

    /**
     * Creates a view model with no results or message.
     */
    public SearchRecipesViewModel() {
    }

    /**
     * Returns the current search results.
     *
     * @return an unmodifiable list of recipe snapshots
     */
    public List<Recipe> getRecipes() {
        return recipes;
    }

    /**
     * Returns the current informational or error message.
     *
     * @return the current message, initially an empty string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Registers a callback for subsequent state updates.
     * The callback runs on the thread that performs the update.
     *
     * @param listener the callback to register
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void setResults(List<Recipe> recipes, String message) {
        this.recipes = List.copyOf(recipes);
        this.message = message;
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
