package use_case.load_cooking_history;

import entity.CookingRecord;

import java.time.LocalDate;
import java.util.List;

/** Loads completed meals for the history page. */
public interface LoadCookingHistoryInputBoundary {
    void execute();

    interface DataAccess {
        List<CookingRecord> loadHistory();
    }

    interface OutputBoundary {
        void showHistory(List<HistoryEntry> entries);
        void showHistoryError(String message);
    }

    /** The details displayed for each completed meal. */
    record HistoryEntry(LocalDate cookedOn, String recipeName) {
    }
}
