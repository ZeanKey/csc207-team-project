package interface_adapter.update_stock_level;

import entity.StockLevel;
import use_case.update_stock_level.UpdateStockLevelInputBoundary;
import use_case.update_stock_level.UpdateStockLevelInputData;

/**
 * Sends a fridge stock-level change to the use case.
 */
public final class UpdateStockLevelController {
    private final UpdateStockLevelInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the stock update input boundary
     */
    public UpdateStockLevelController(UpdateStockLevelInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Changes one ingredient's stock level.
     *
     * @param ingredientName the selected ingredient
     * @param stockLevel the new stock level
     */
    public void execute(String ingredientName, StockLevel stockLevel) {
        interactor.execute(new UpdateStockLevelInputData(ingredientName, stockLevel));
    }
}
