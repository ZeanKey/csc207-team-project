package interface_adapter.cooking;

import use_case.complete_meal.CompleteMealOutputBoundary;
import use_case.complete_meal.CompleteMealOutputData;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary.HistoryEntry;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary.PendingMeal;
import use_case.prepare_checklist.PrepareChecklistInputBoundary;
import use_case.prepare_checklist.PrepareChecklistInputBoundary.ChecklistItem;

import java.util.List;

/** Translates cooking results into view model updates. */
public final class CookingPresenter implements LoadUncookedMealsInputBoundary.OutputBoundary,
        PrepareChecklistInputBoundary.OutputBoundary, CompleteMealOutputBoundary,
        LoadCookingHistoryInputBoundary.OutputBoundary {
    private final CookingViewModel viewModel;

    public CookingPresenter(CookingViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void showMeals(List<PendingMeal> meals) {
        viewModel.setMeals(meals);
        viewModel.setStatus(meals.isEmpty() ? "No meals to cook. Add one in Meal Plan." : "", false);
    }

    @Override
    public void showMealsError(String message) {
        viewModel.setMeals(List.of());
        viewModel.setStatus(message, false);
    }

    @Override
    public void showChecklist(List<ChecklistItem> items) {
        viewModel.setChecklist(items);
    }

    @Override
    public void showChecklistError(String message, boolean refreshRequired) {
        viewModel.setStatus(message, refreshRequired);
    }

    @Override
    public void prepareSuccessView(CompleteMealOutputData outputData) {
        viewModel.setStatus(outputData.message(), outputData.refreshRequired());
    }

    @Override
    public void prepareFailView(CompleteMealOutputData outputData) {
        viewModel.setStatus(outputData.message(), outputData.refreshRequired());
    }

    @Override
    public void showHistory(List<HistoryEntry> entries) {
        viewModel.setHistory(entries, entries.isEmpty() ? "No meals cooked yet." : "");
    }

    @Override
    public void showHistoryError(String message) {
        viewModel.setHistory(List.of(), message);
    }
}
