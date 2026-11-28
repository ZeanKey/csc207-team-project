package view;

import interface_adapter.cooking.CookingController;
import interface_adapter.cooking.CookingViewModel;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary.HistoryEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Lists the names and dates of completed meals. */
public final class CookingHistoryView extends JPanel {
    private final CookingViewModel viewModel;
    private final CookingController controller;
    private final DefaultListModel<String> rows = new DefaultListModel<>();
    private final JLabel status = new JLabel(" ");

    public CookingHistoryView(CookingViewModel viewModel, CookingController controller, Runnable back) {
        super(new BorderLayout(0, 12));
        this.viewModel = viewModel;
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        final JButton backButton = new JButton("Back to Cook");
        backButton.addActionListener(event -> back.run());
        final JPanel header = new JPanel(new BorderLayout(12, 0));
        header.add(backButton, BorderLayout.WEST);
        final JLabel title = new JLabel("Cooking history");
        final JList<String> list = new JList<>(rows);
        title.setLabelFor(list);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        viewModel.addListener(CookingViewModel.HISTORY, event -> showHistory());
    }

    public void onShown() {
        controller.loadHistory();
    }

    private void showHistory() {
        rows.clear();
        final DateTimeFormatter date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        for (HistoryEntry entry : viewModel.getHistory()) {
            rows.addElement(date.format(entry.cookedOn()) + "  •  " + entry.recipeName());
        }
        status.setText(viewModel.getHistoryMessage());
    }
}
