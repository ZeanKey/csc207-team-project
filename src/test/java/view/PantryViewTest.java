package view;

import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import data_access.FilePantryDataAccessObject;
import entity.IngredientName;
import entity.Pantry;
import entity.PantryItem;
import entity.StockLevel;
import interface_adapter.load_pantry.LoadPantryController;
import interface_adapter.load_pantry.LoadPantryPresenter;
import interface_adapter.pantry.PantryViewModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.load_pantry.LoadPantryInteractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PantryViewTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    private Path directory;

    @Test
    void showsTheSavedInventoryAndReloadsWhenShownAgain() throws Exception {
        final FilePantryDataAccessObject dao =
                new FilePantryDataAccessObject(directory.resolve("pantry.json"));
        final LocalDate today = LocalDate.now(CLOCK);
        final Pantry inventory = new Pantry();
        inventory.add(new PantryItem(IngredientName.fromRawName("Garlic"), null, StockLevel.ENOUGH));
        inventory.add(new PantryItem(IngredientName.fromRawName("Milk"), today.plusDays(2),
                StockLevel.LOW));
        inventory.add(new PantryItem(IngredientName.fromRawName("Spinach"), today.minusDays(1),
                StockLevel.ENOUGH));
        dao.savePantry(inventory);

        SwingUtilities.invokeAndWait(() -> {
            final PantryView view = buildView(dao);
            final BorderLayout layout = (BorderLayout) view.getLayout();
            final JScrollPane scroll = (JScrollPane) layout.getLayoutComponent(BorderLayout.CENTER);
            final JTable table = (JTable) scroll.getViewport().getView();

            view.onShown();

            assertEquals(3, table.getRowCount());
            assertEquals("Spinach", table.getValueAt(0, 0));
            assertEquals("Expired", table.getValueAt(0, 3));
            assertEquals("Milk", table.getValueAt(1, 0));
            assertEquals(today.plusDays(2).toString(), table.getValueAt(1, 1));
            assertEquals("Running low", table.getValueAt(1, 2));
            assertEquals("Expiring soon", table.getValueAt(1, 3));
            assertTrue(table.prepareRenderer(table.getCellRenderer(1, 3), 1, 3).getFont().isBold());
            assertEquals("Garlic", table.getValueAt(2, 0));
            assertEquals("—", table.getValueAt(2, 1));
            assertFalse(table.isCellEditable(1, 0));

            dao.savePantry(new Pantry());
            view.onShown();

            assertEquals(0, table.getRowCount());
            assertEquals("No ingredients in the fridge.", statusLabel(view).getText());
        });
    }

    @Test
    void showsAnErrorWhenTheSavedInventoryCannotBeRead() throws Exception {
        final Path file = directory.resolve("pantry.json");
        Files.writeString(file, "{invalid");
        final FilePantryDataAccessObject dao = new FilePantryDataAccessObject(file);

        SwingUtilities.invokeAndWait(() -> {
            final PantryView view = buildView(dao);
            view.onShown();
            assertEquals("Could not load the fridge inventory.", statusLabel(view).getText());
        });
    }

    private static PantryView buildView(FilePantryDataAccessObject dao) {
        final PantryViewModel model = new PantryViewModel();
        final LoadPantryController controller = new LoadPantryController(
                new LoadPantryInteractor(dao, new LoadPantryPresenter(model), CLOCK));
        return new PantryView(model, controller);
    }

    private static JLabel statusLabel(PantryView view) {
        return (JLabel) ((BorderLayout) view.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
    }
}
