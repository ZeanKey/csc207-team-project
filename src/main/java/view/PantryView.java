package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import interface_adapter.load_pantry.LoadPantryController;
import interface_adapter.pantry.PantryItemViewData;
import interface_adapter.pantry.PantryViewModel;

/**
 * Shows the saved fridge inventory and highlights items nearing expiry.
 */
public final class PantryView extends JPanel {
    private static final String EXPIRING_SOON = "Expiring soon";

    private final PantryViewModel viewModel;
    private final LoadPantryController loadController;
    private final PantryTableModel tableModel = new PantryTableModel();
    private final JLabel statusLabel = new JLabel(" ");

    /**
     * Creates the fridge page.
     *
     * @param viewModel the inventory shown by the page
     * @param loadController the controller for loading the saved inventory
     */
    public PantryView(PantryViewModel viewModel, LoadPantryController loadController) {
        super(new BorderLayout(8, 8));
        this.viewModel = viewModel;
        this.loadController = loadController;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        final JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(table.getColumnCount() - 1)
                .setCellRenderer(new ExpiryStatusRenderer());

        final JLabel heading = new JLabel("Fridge");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        heading.setLabelFor(table);
        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        viewModel.addListener(this::refreshRows);
        refreshRows();
    }

    /**
     * Reloads the inventory whenever the fridge page becomes visible.
     */
    public void onShown() {
        try {
            loadController.execute();
        }
        catch (IllegalStateException exception) {
            statusLabel.setText("Could not load the fridge inventory.");
        }
    }

    private void refreshRows() {
        final List<PantryItemViewData> items = viewModel.getItems();
        tableModel.updateItems(items);
        statusLabel.setText(items.isEmpty() ? "No ingredients in the fridge." : " ");
    }

    private static final class PantryTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
            "Ingredient", "Expiry date", "Stock level", "Expiry status"
        };

        private List<PantryItemViewData> items = List.of();

        private void updateItems(List<PantryItemViewData> items) {
            this.items = items;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            final PantryItemViewData item = items.get(row);
            return switch (column) {
                case 0 -> item.displayName();
                case 1 -> item.expiryDate() == null ? "—" : item.expiryDate().toString();
                case 2 -> switch (item.stockLevel()) {
                    case ENOUGH -> "Enough";
                    case LOW -> "Running low";
                };
                case 3 -> expiryStatus(item);
                default -> throw new IndexOutOfBoundsException(column);
            };
        }

        private static String expiryStatus(PantryItemViewData item) {
            if (item.expired()) {
                return "Expired";
            }
            return item.expiringSoon() ? EXPIRING_SOON : "";
        }
    }

    private static final class ExpiryStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean hasFocus, int row, int column) {
            final Component cell = super.getTableCellRendererComponent(
                    table, value, selected, hasFocus, row, column);
            final Font font = table.getFont();
            cell.setFont(EXPIRING_SOON.equals(value) ? font.deriveFont(Font.BOLD) : font);
            return cell;
        }
    }
}
