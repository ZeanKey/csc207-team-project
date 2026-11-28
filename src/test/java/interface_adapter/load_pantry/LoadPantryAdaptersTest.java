package interface_adapter.load_pantry;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import entity.IngredientName;
import entity.Pantry;
import entity.PantryItem;
import entity.StockLevel;
import interface_adapter.pantry.PantryItemViewData;
import interface_adapter.pantry.PantryViewModel;
import org.junit.jupiter.api.Test;
import use_case.load_pantry.LoadPantryInteractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadPantryAdaptersTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void reloadPublishesOrderedRowsAndClearsThemWhenThePantryBecomesEmpty() {
        final LocalDate today = LocalDate.now(CLOCK);
        final IngredientName garlic = IngredientName.fromRawName("Garlic");
        final IngredientName milk = IngredientName.fromRawName("Fresh_Milk");
        final IngredientName spinach = IngredientName.fromRawName("Spinach");
        final Pantry pantry = new Pantry();
        pantry.add(new PantryItem(garlic, null, StockLevel.ENOUGH));
        pantry.add(new PantryItem(milk, today.plusDays(3), StockLevel.LOW));
        pantry.add(new PantryItem(spinach, today.minusDays(1), StockLevel.ENOUGH));

        final PantryViewModel viewModel = new PantryViewModel();
        final List<List<PantryItemViewData>> publishedRows = new ArrayList<>();
        viewModel.addListener(() -> publishedRows.add(viewModel.getItems()));
        final LoadPantryController controller = new LoadPantryController(
                new LoadPantryInteractor(() -> pantry, new LoadPantryPresenter(viewModel), CLOCK));

        controller.execute();
        final List<PantryItemViewData> expectedRows = List.of(
                new PantryItemViewData("Spinach", "spinach", today.minusDays(1),
                        StockLevel.ENOUGH, true, false),
                new PantryItemViewData("Fresh_Milk", "fresh milk", today.plusDays(3),
                        StockLevel.LOW, false, true),
                new PantryItemViewData("Garlic", "garlic", null, StockLevel.ENOUGH, false, false));
        assertEquals(expectedRows, viewModel.getItems());

        pantry.remove(garlic);
        pantry.remove(milk);
        pantry.remove(spinach);
        controller.execute();

        assertEquals(List.of(), viewModel.getItems());
        assertEquals(List.of(expectedRows, List.of()), publishedRows);
    }
}
