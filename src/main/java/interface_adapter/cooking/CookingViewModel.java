package interface_adapter.cooking;

import use_case.load_cooking_history.LoadCookingHistoryInputBoundary.HistoryEntry;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary.PendingMeal;
import use_case.prepare_checklist.PrepareChecklistInputBoundary.ChecklistItem;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

/** Publishes the results displayed by Cook and History. */
public final class CookingViewModel {
    public static final String MEALS = "meals";
    public static final String CHECKLIST = "checklist";
    public static final String STATUS = "status";
    public static final String HISTORY = "history";

    /** A short status message and whether the pending meal list should reload. */
    public record Status(String message, boolean reloadMeals) {
    }

    private final PropertyChangeSupport events = new PropertyChangeSupport(this);
    private List<PendingMeal> meals = List.of();
    private List<ChecklistItem> checklist = List.of();
    private List<HistoryEntry> history = List.of();
    private String historyMessage = "";
    private Status status = new Status("", false);

    public void addListener(String property, PropertyChangeListener listener) {
        events.addPropertyChangeListener(property, listener);
    }

    public List<PendingMeal> getMeals() {
        return meals;
    }

    void setMeals(List<PendingMeal> meals) {
        this.meals = List.copyOf(meals);
        events.firePropertyChange(MEALS, null, this.meals);
    }

    public List<ChecklistItem> getChecklist() {
        return checklist;
    }

    void setChecklist(List<ChecklistItem> checklist) {
        this.checklist = List.copyOf(checklist);
        events.firePropertyChange(CHECKLIST, null, this.checklist);
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public String getHistoryMessage() {
        return historyMessage;
    }

    void setHistory(List<HistoryEntry> history, String message) {
        this.history = List.copyOf(history);
        historyMessage = message;
        events.firePropertyChange(HISTORY, null, this.history);
    }

    public Status getStatus() {
        return status;
    }

    void setStatus(String message, boolean reloadMeals) {
        status = new Status(message, reloadMeals);
        events.firePropertyChange(STATUS, null, status);
    }
}
