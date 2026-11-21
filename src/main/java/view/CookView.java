package view;

import entity.IngredientName;
import entity.StockLevel;
import interface_adapter.cooking.CookingController;
import interface_adapter.cooking.CookingViewModel;
import use_case.complete_meal.CompleteMealInputData.StockChoice;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary.PendingMeal;
import use_case.prepare_checklist.PrepareChecklistInputBoundary.ChecklistItem;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Shows the meals waiting to be cooked and opens their confirmation checklist. */
public final class CookView extends JPanel {
    private static final StockOption USED_UP = new StockOption("Used up", StockChoice.USED_UP);
    private static final StockOption RUNNING_LOW = new StockOption("Running low", StockChoice.RUNNING_LOW);
    private static final StockOption STILL_ENOUGH = new StockOption("Still enough", StockChoice.STILL_ENOUGH);

    private final CookingViewModel viewModel;
    private final CookingController controller;
    private final DefaultListModel<PendingMeal> meals = new DefaultListModel<>();
    private final JList<PendingMeal> mealList = new JList<>(meals);
    private final JButton cook = new JButton("I cooked this");
    private final JLabel status = new JLabel(" ");

    public CookView(CookingViewModel viewModel, CookingController controller, Runnable openHistory) {
        super(new BorderLayout(0, 12));
        this.viewModel = viewModel;
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        final JLabel title = new JLabel("Meals to cook this week");
        title.setLabelFor(mealList);
        add(title, BorderLayout.NORTH);
        mealList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mealList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof PendingMeal meal) {
                    final String day = meal.slot().getDay().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                    final String type = meal.slot().getMealType().name().toLowerCase(Locale.ROOT);
                    setText(day + " " + type + "  •  " + meal.recipeName());
                }
                return this;
            }
        });
        add(new JScrollPane(mealList), BorderLayout.CENTER);

        cook.setEnabled(false);
        mealList.addListSelectionListener(event -> cook.setEnabled(!mealList.isSelectionEmpty()));
        cook.addActionListener(event -> controller.prepareChecklist(mealList.getSelectedValue()));
        final JButton history = new JButton("History");
        history.addActionListener(event -> openHistory.run());
        final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(history);
        actions.add(cook);
        final JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.add(status, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        viewModel.addListener(CookingViewModel.MEALS, event -> showMeals());
        viewModel.addListener(CookingViewModel.CHECKLIST, event -> showChecklist());
        viewModel.addListener(CookingViewModel.STATUS, event -> showStatus());
    }

    public void onShown() {
        controller.loadMeals();
    }

    private void showMeals() {
        meals.clear();
        meals.addAll(viewModel.getMeals());
        if (!meals.isEmpty()) {
            mealList.setSelectedIndex(0);
        }
    }

    private void showStatus() {
        final CookingViewModel.Status result = viewModel.getStatus();
        if (result.reloadMeals()) {
            controller.loadMeals();
        }
        final String currentMessage = status.getText().trim();
        final String message = result.reloadMeals() && !currentMessage.isEmpty()
                ? result.message() + " " + currentMessage : result.message();
        status.setText(message.isEmpty() ? " " : message);
    }

    private void showChecklist() {
        final PendingMeal meal = mealList.getSelectedValue();
        final JPanel items = new JPanel();
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        final Map<IngredientName, JComboBox<StockOption>> choices = new LinkedHashMap<>();

        for (ChecklistItem item : viewModel.getChecklist()) {
            final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            final String measure = item.measure();
            final JLabel label = new JLabel(item.name().getDisplayName()
                    + (measure == null || measure.isBlank() ? "" : " (" + measure + ")"));
            row.add(label);
            if (item.inPantry()) {
                final JComboBox<StockOption> choice = new JComboBox<>(
                        new StockOption[]{USED_UP, RUNNING_LOW, STILL_ENOUGH});
                choice.setSelectedItem(item.stockLevel() == StockLevel.LOW ? RUNNING_LOW : STILL_ENOUGH);
                label.setLabelFor(choice);
                choices.put(item.name(), choice);
                row.add(choice);
            }
            else {
                row.add(new JLabel("Not in fridge"));
            }
            items.add(row);
        }
        if (viewModel.getChecklist().isEmpty()) {
            items.add(new JLabel("This recipe has no listed ingredients."));
        }

        final JScrollPane checklist = new JScrollPane(items);
        checklist.setPreferredSize(new Dimension(380, Math.min(300, items.getPreferredSize().height + 20)));
        final int answer = JOptionPane.showOptionDialog(this, checklist, "Confirm " + meal.recipeName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Confirm", "Cancel"}, "Cancel");
        if (answer == JOptionPane.OK_OPTION) {
            final Map<IngredientName, StockChoice> selected = new LinkedHashMap<>();
            choices.forEach((name, choice) -> selected.put(name, choice.getItemAt(choice.getSelectedIndex()).value()));
            controller.completeMeal(meal, selected);
        }
    }

    private record StockOption(String label, StockChoice value) {
        @Override
        public String toString() {
            return label;
        }
    }
}
