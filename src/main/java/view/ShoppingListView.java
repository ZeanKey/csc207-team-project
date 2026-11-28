package view;

import interface_adapter.generate_shopping_list.GenerateShoppingListController;
import interface_adapter.load_shopping_list.LoadShoppingListController;
import interface_adapter.shopping_list.ShoppingListItemViewData;
import interface_adapter.shopping_list.ShoppingListState;
import interface_adapter.shopping_list.ShoppingListViewModel;
import interface_adapter.toggle_bought.ToggleBoughtController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Swing view for the shopping list page.
 */
public class ShoppingListView extends JPanel
        implements ShoppingListViewModel.ShoppingListViewModelListener {

    /** Space between the page's top, centre and bottom areas, in pixels. */
    private static final int GAP = 8;
    /** Empty border around the whole page, in pixels. */
    private static final int PADDING = 12;
    /** Font size of the "Shopping List" title. */
    private static final float TITLE_FONT_SIZE = 18f;
    /** Space between the "Missing" and "Running low" sections, in pixels. */
    private static final int SECTION_GAP = 12;
    /** Space between two rows of a section, in pixels. */
    private static final int ROW_GAP = 4;

    private final ShoppingListViewModel viewModel;
    private final LoadShoppingListController loadController;
    private final GenerateShoppingListController generateController;
    private final ToggleBoughtController toggleBoughtController;

    private final JPanel missingPanel = new JPanel();
    private final JPanel lowPanel = new JPanel();
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel errorLabel = new JLabel(" ");

    /**
     * Creates the shopping list view.
     *
     * @param viewModel the view model
     * @param loadController the load controller
     * @param generateController the generate controller
     * @param toggleBoughtController the toggle controller
     */
    public ShoppingListView(ShoppingListViewModel viewModel,
                            LoadShoppingListController loadController,
                            GenerateShoppingListController generateController,
                            ToggleBoughtController toggleBoughtController) {
        this.viewModel = viewModel;
        this.loadController = loadController;
        this.generateController = generateController;
        this.toggleBoughtController = toggleBoughtController;

        setLayout(new BorderLayout(GAP, GAP));
        setBorder(BorderFactory.createEmptyBorder(
                PADDING, PADDING, PADDING, PADDING));

        JLabel title = new JLabel("Shopping List");
        title.setFont(title.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));

        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(event -> generateController.execute());

        JPanel top = new JPanel(new BorderLayout());
        top.add(title, BorderLayout.WEST);
        top.add(generateButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        missingPanel.setLayout(new BoxLayout(missingPanel, BoxLayout.Y_AXIS));
        lowPanel.setLayout(new BoxLayout(lowPanel, BoxLayout.Y_AXIS));

        JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.add(section("Missing", missingPanel));
        sections.add(Box.createVerticalStrut(SECTION_GAP));
        sections.add(section("Running low", lowPanel));
        add(new JScrollPane(sections), BorderLayout.CENTER);

        errorLabel.setForeground(Color.RED.darker());
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(messageLabel);
        bottom.add(errorLabel);
        add(bottom, BorderLayout.SOUTH);

        viewModel.addListener(this);
        refreshFromState(viewModel.getState());
    }

    /**
     * Called by ViewManager whenever this view becomes visible.
     */
    public void onShown() {
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        loadController.execute(weekStart);
    }

    @Override
    public void stateChanged() {
        refreshFromState(viewModel.getState());
    }

    private void refreshFromState(ShoppingListState state) {
        rebuildSection(
                missingPanel, state.getMissingItems(), state.getWeekStart());
        rebuildSection(lowPanel, state.getLowItems(), state.getWeekStart());
        messageLabel.setText(blankToSpace(state.getMessage()));
        errorLabel.setText(blankToSpace(state.getError()));
        revalidate();
        repaint();
    }

    private void rebuildSection(JPanel panel,
                                List<ShoppingListItemViewData> items,
                                LocalDate weekStart) {
        panel.removeAll();
        if (items.isEmpty()) {
            panel.add(new JLabel("(none)"));
            return;
        }
        for (ShoppingListItemViewData item : items) {
            panel.add(createRow(item, weekStart));
            panel.add(Box.createVerticalStrut(ROW_GAP));
        }
    }

    private JPanel createRow(
            ShoppingListItemViewData item, LocalDate weekStart) {
        JPanel row = new JPanel(new BorderLayout());
        JCheckBox bought = new JCheckBox(item.getName(), item.isBought());
        bought.addActionListener(event -> {
            if (weekStart != null) {
                toggleBoughtController.execute(weekStart, item.getName());
            }
        });
        JLabel neededFor = new JLabel("for: " + item.getNeededFor());
        neededFor.setForeground(Color.DARK_GRAY);
        row.add(bought, BorderLayout.WEST);
        row.add(neededFor, BorderLayout.CENTER);
        return row;
    }

    private static JPanel section(String title, JPanel content) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static String blankToSpace(String text) {
        if (text == null || text.isBlank()) {
            return " ";
        }
        return text;
    }
}
