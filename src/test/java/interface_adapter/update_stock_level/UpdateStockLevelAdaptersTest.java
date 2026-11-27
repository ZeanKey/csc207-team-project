package interface_adapter.update_stock_level;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import data_access.FilePantryDataAccessObject;
import entity.IngredientName;
import entity.Pantry;
import entity.PantryItem;
import entity.StockLevel;
import interface_adapter.load_pantry.LoadPantryController;
import interface_adapter.load_pantry.LoadPantryPresenter;
import interface_adapter.pantry.PantryItemViewData;
import interface_adapter.pantry.PantryViewModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.load_pantry.LoadPantryInteractor;
import use_case.update_stock_level.UpdateStockLevelInteractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateStockLevelAdaptersTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-27T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void publishesFailuresAndUpdatesOnlyTheSavedRowOnSuccess(@TempDir Path directory) {
        final Path file = directory.resolve("pantry.json");
        final FilePantryDataAccessObject dao = new FilePantryDataAccessObject(file);
        final IngredientName milk = IngredientName.fromRawName("Fresh Milk");
        final IngredientName garlic = IngredientName.fromRawName("Garlic");
        final LocalDate expiry = LocalDate.now(CLOCK).plusDays(2);
        final Pantry saved = new Pantry();
        saved.add(new PantryItem(milk, expiry, StockLevel.ENOUGH));
        saved.add(new PantryItem(garlic, null, StockLevel.ENOUGH));
        dao.savePantry(saved);

        final PantryViewModel model = new PantryViewModel();
        new LoadPantryController(new LoadPantryInteractor(
                dao, new LoadPantryPresenter(model), CLOCK)).execute();
        final List<PantryItemViewData> originalRows = model.getItems();
        final List<String> observedErrors = new ArrayList<>();
        model.addListener(() -> observedErrors.add(model.getError()));
        final UpdateStockLevelController controller = new UpdateStockLevelController(
                new UpdateStockLevelInteractor(dao, new UpdateStockLevelPresenter(model)));

        controller.execute("Missing", StockLevel.LOW);

        final String error = "Ingredient is not in the pantry.";
        assertEquals(error, model.getError());
        assertEquals(originalRows, model.getItems());

        controller.execute("  fresh_milk  ", StockLevel.LOW);

        final PantryItemViewData updated = new PantryItemViewData(
                "Fresh Milk", "fresh milk", expiry, StockLevel.LOW, false, true);
        assertEquals(List.of(updated, originalRows.get(1)), model.getItems());
        assertEquals("", model.getError());
        assertEquals(List.of(error, ""), observedErrors);
        final Pantry reloaded = new FilePantryDataAccessObject(file).loadPantry();
        assertEquals(StockLevel.LOW, reloaded.find(milk).orElseThrow().getStockLevel());
    }
}
