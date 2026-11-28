package interface_adapter.pantry;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the current fridge rows and notifies listeners after each load.
 */
public final class PantryViewModel {
    private final List<Runnable> listeners = new ArrayList<>();
    private List<PantryItemViewData> items = List.of();

    /**
     * Returns the current rows in expiry order.
     */
    public List<PantryItemViewData> getItems() {
        return items;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /**
     * Replaces the current rows before notifying listeners.
     */
    public void updateItems(List<PantryItemViewData> items) {
        this.items = List.copyOf(items);
        List.copyOf(listeners).forEach(Runnable::run);
    }
}
