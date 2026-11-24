package view;

import entity.Recipe;
import interface_adapter.search_recipes.SearchRecipesController;
import interface_adapter.search_recipes.SearchRecipesViewModel;
import use_case.search_recipes.RecipeSearchType;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Search form and result list, usable inside an application or a standalone demo.
 */
public final class RecipeSearchView extends JPanel {
    private static final String SEARCH_BUTTON_TEXT = "Search";
    private static final String SEARCH_FIELD_LABEL_TEXT = "Search recipes:";
    private static final String SEARCH_INTERRUPTED_TEXT = "Search interrupted. Please try again.";
    private static final String SEARCH_FAILED_TEXT = "Search failed. Please try again.";
    private static final String SEARCH_PENDING_TEXT = "Searching...";

    private final SearchRecipesViewModel viewModel;
    private final SearchRecipesController controller;
    private final JTextField keyword = new JTextField(20);
    private final JComboBox<RecipeSearchType> searchType = new JComboBox<>(RecipeSearchType.values());
    private final JButton search = new JButton(SEARCH_BUTTON_TEXT);
    private final JLabel message = new JLabel(" ");
    private final JPanel results = new JPanel();

    /**
     * Creates a search form and connects it to its view model and controller.
     *
     * @param viewModel the model supplying results and messages
     * @param controller the controller handling search requests
     * @throws NullPointerException if either argument is null
     */
    public RecipeSearchView(SearchRecipesViewModel viewModel, SearchRecipesController controller) {
        super(new BorderLayout(8, 8));
        this.viewModel = Objects.requireNonNull(viewModel);
        this.controller = Objects.requireNonNull(controller);

        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel controls = new JPanel();
        controls.add(new JLabel(SEARCH_FIELD_LABEL_TEXT));
        controls.add(keyword);
        controls.add(searchType);
        controls.add(search);
        add(controls, BorderLayout.NORTH);
        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        add(new JScrollPane(results), BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);

        search.addActionListener(event -> startSearch());
        keyword.addActionListener(event -> { if (search.isEnabled()) { startSearch(); } });
        viewModel.addListener(() -> {
            if (SwingUtilities.isEventDispatchThread()) {
                refresh();
            }
            else {
                SwingUtilities.invokeLater(this::refresh);
            }
        });
        refresh();
    }

    private void startSearch() {
        String text = keyword.getText();
        RecipeSearchType type = (RecipeSearchType) searchType.getSelectedItem();
        search.setEnabled(false);
        message.setText(SEARCH_PENDING_TEXT);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                controller.execute(text, type);
                return null;
            }

            @Override
            protected void done() {
                search.setEnabled(true);
                try {
                    get();
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    message.setText(SEARCH_INTERRUPTED_TEXT);
                }
                catch (ExecutionException exception) {
                    message.setText(SEARCH_FAILED_TEXT);
                }
            }
        }.execute();
    }

    private void refresh() {
        results.removeAll();
        message.setText(viewModel.getMessage().isEmpty() ? " " : viewModel.getMessage());
        for (Recipe recipe : viewModel.getRecipes()) {
            JPanel row = new JPanel(new BorderLayout(8, 8));
            RecipeImageView image = new RecipeImageView(96, 72);
            row.add(image, BorderLayout.WEST);
            row.add(new JLabel(recipe.getName()), BorderLayout.CENTER);
            results.add(row);
            image.setImage(recipe.getThumbnail());
        }
        results.revalidate();
        results.repaint();
    }
}
