package interface_adapter.load_pantry;

import java.util.List;

import interface_adapter.pantry.PantryItemViewData;
import interface_adapter.pantry.PantryViewModel;
import use_case.load_pantry.LoadPantryItemOutputData;
import use_case.load_pantry.LoadPantryOutputBoundary;
import use_case.load_pantry.LoadPantryOutputData;

/**
 * Publishes the loaded inventory to the fridge view model.
 */
public final class LoadPantryPresenter implements LoadPantryOutputBoundary {
    private final PantryViewModel viewModel;

    public LoadPantryPresenter(PantryViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(LoadPantryOutputData outputData) {
        final List<PantryItemViewData> items = outputData.getItems().stream()
                .map(LoadPantryPresenter::toViewData)
                .toList();
        viewModel.updateItems(items);
    }

    private static PantryItemViewData toViewData(LoadPantryItemOutputData item) {
        return new PantryItemViewData(item.getDisplayName(), item.getIngredientKey(),
                item.getExpiryDate(), item.getStockLevel(), item.isExpired(), item.isExpiringSoon());
    }
}
