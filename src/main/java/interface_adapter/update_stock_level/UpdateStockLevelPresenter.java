package interface_adapter.update_stock_level;

import java.util.List;

import entity.IngredientName;
import entity.StockLevel;
import interface_adapter.pantry.PantryItemViewData;
import interface_adapter.pantry.PantryViewModel;
import use_case.update_stock_level.UpdateStockLevelOutputBoundary;
import use_case.update_stock_level.UpdateStockLevelOutputData;

/**
 * Publishes stock update results to the fridge view model.
 */
public final class UpdateStockLevelPresenter implements UpdateStockLevelOutputBoundary {
    private final PantryViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the fridge view model to update
     */
    public UpdateStockLevelPresenter(PantryViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(UpdateStockLevelOutputData outputData) {
        final String key = IngredientName.fromRawName(outputData.getIngredientName()).getKey();
        final StockLevel stockLevel = outputData.getStockLevel();
        final List<PantryItemViewData> items = viewModel.getItems().stream()
                .map(item -> item.ingredientKey().equals(key)
                        ? new PantryItemViewData(item.displayName(), item.ingredientKey(),
                                item.expiryDate(), stockLevel, item.expired(), item.expiringSoon())
                        : item)
                .toList();
        viewModel.updateItems(items);
    }

    @Override
    public void prepareFailView(UpdateStockLevelOutputData outputData) {
        viewModel.showError(outputData.getMessage());
    }
}
