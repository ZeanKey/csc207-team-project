package interface_adapter.search_recipes;

import java.util.ArrayList;
import java.util.List;

/**
 * State of the Recipes page: the result rows and the message under them.
 */
public class SearchRecipesState {
    private List<RecipeSearchResultViewData> results = new ArrayList<>();
    private String message = "";

    /**
     * Returns the result rows, in search order.
     *
     * @return the rows; empty before the first search or when nothing matched
     */
    public List<RecipeSearchResultViewData> getResults() {
        return results;
    }

    /**
     * Replaces the result rows.
     *
     * @param results the rows, in search order
     */
    public void setResults(List<RecipeSearchResultViewData> results) {
        this.results = new ArrayList<>(results);
    }

    /**
     * Returns the info or error message to show, or an empty string.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the info or error message.
     *
     * @param message the message, or null for none
     */
    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }
}
