package interface_adapter.search_recipes;

import entity.Recipe;
import java.util.ArrayList;
import java.util.List;

/** State observed by views displaying recipe search results. Listeners run on the updating thread. */
public final class SearchRecipesViewModel {
    private final List<Runnable> listeners = new ArrayList<>();
    private volatile List<Recipe> recipes = List.of();
    private volatile String message = "";

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public String getMessage() {
        return message;
    }

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
