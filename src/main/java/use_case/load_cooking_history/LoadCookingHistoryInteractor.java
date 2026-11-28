package use_case.load_cooking_history;

import entity.CookingRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Shows the most recently completed meals first. */
public final class LoadCookingHistoryInteractor implements LoadCookingHistoryInputBoundary {
    private final DataAccess dataAccess;
    private final OutputBoundary presenter;

    public LoadCookingHistoryInteractor(DataAccess dataAccess, OutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute() {
        final List<CookingRecord> history;
        try {
            history = new ArrayList<>(dataAccess.loadHistory());
        }
        catch (IllegalStateException exception) {
            presenter.showHistoryError("Could not load cooking history. Try again.");
            return;
        }

        // Preserve newest append order for meals cooked on the same day.
        Collections.reverse(history);
        final List<HistoryEntry> entries = history.stream()
                .sorted(Comparator.comparing(CookingRecord::cookedOn).reversed())
                .map(record -> new HistoryEntry(record.cookedOn(), record.recipeName()))
                .toList();
        presenter.showHistory(entries);
    }
}
