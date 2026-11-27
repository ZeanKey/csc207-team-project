package interface_adapter.pantry;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the current fridge rows and any error to display.
 */
public final class PantryViewModel {
    private final List<Runnable> listeners = new ArrayList<>();
    private List<PantryItemViewData> items = List.of();
    private String error = "";

    /**
     * Returns the current rows in expiry order.
     */
    public List<PantryItemViewData> getItems() {
        return items;
    }

    /**
     * Returns the error to display, or an empty string when there is none.
     *
     * @return the current error
     */
    public String getError() {
        return error;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /**
     * Replaces the rows and clears the previous error before notifying listeners.
     */
    public void updateItems(List<PantryItemViewData> items) {
        this.items = List.copyOf(items);
        error = "";
        notifyListeners();
    }

    /**
     * Reports a failure while keeping the current rows.
     *
     * @param message the error to display
     */
    public void showError(String message) {
        error = message;
        notifyListeners();
    }

    private void notifyListeners() {
        List.copyOf(listeners).forEach(Runnable::run);
    }
}
