package view;

import interface_adapter.search_recipes.RecipeSearchResultViewData;
import interface_adapter.search_recipes.SearchRecipesController;
import interface_adapter.search_recipes.SearchRecipesState;
import interface_adapter.search_recipes.SearchRecipesViewModel;
import interface_adapter.search_recipes.SearchRecipesViewModelListener;
import use_case.search_recipes.RecipeSearchType;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * The View for the Recipes page: a search form and the list of results.
 */
public final class RecipeSearchView extends JPanel implements SearchRecipesViewModelListener {
    private static final String SEARCH_BUTTON_TEXT = "Search";
    private static final String SEARCH_FIELD_LABEL_TEXT = "Search recipes:";
    private static final String SEARCH_INTERRUPTED_TEXT = "Search interrupted. Please try again.";
    private static final String SEARCH_FAILED_TEXT = "Search failed. Please try again.";
    private static final String SEARCH_PENDING_TEXT = "Searching...";
    private static final int THUMBNAIL_WIDTH = 96;
    private static final int THUMBNAIL_HEIGHT = 72;

    private final SearchRecipesViewModel viewModel;
    private final JTextField keyword = new JTextField(20);
    private final JComboBox<RecipeSearchType> searchType = new JComboBox<>(RecipeSearchType.values());
    private final JButton search = new JButton(SEARCH_BUTTON_TEXT);
    private final JLabel message = new JLabel(" ");
    private final JPanel results = new JPanel();
    private SearchRecipesController searchRecipesController;

    /**
     * Creates the Recipes page and starts listening to its view model.
     * The controller is supplied afterwards with
     * {@link #setSearchRecipesController(SearchRecipesController)}.
     *
     * @param viewModel the model supplying results and messages
     * @throws NullPointerException if viewModel is null
     */
    public RecipeSearchView(SearchRecipesViewModel viewModel) {
        super(new BorderLayout(8, 8));
        this.viewModel = Objects.requireNonNull(viewModel);
        this.viewModel.addListener(this);

        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        final JPanel controls = new JPanel();
        controls.add(new JLabel(SEARCH_FIELD_LABEL_TEXT));
        controls.add(keyword);
        controls.add(searchType);
        controls.add(search);
        add(controls, BorderLayout.NORTH);
        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        add(new JScrollPane(results), BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);

        search.addActionListener(event -> startSearch());
        keyword.addActionListener(event -> {
            if (search.isEnabled()) {
                startSearch();
            }
        });
        refresh();
    }

    /**
     * Sets the controller that runs the search use case.
     *
     * @param controller the search controller
     */
    public void setSearchRecipesController(SearchRecipesController controller) {
        this.searchRecipesController = controller;
    }

    /**
     * Redraws the page from the view model, on the Swing thread.
     */
    @Override
    public void stateChanged() {
        if (SwingUtilities.isEventDispatchThread()) {
            refresh();
        }
        else {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    private void startSearch() {
        final SearchRecipesController controller = searchRecipesController;
        if (controller == null) {
            return;
        }
        final String text = keyword.getText();
        final RecipeSearchType type = (RecipeSearchType) searchType.getSelectedItem();
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
        final SearchRecipesState state = viewModel.getState();
        results.removeAll();
        message.setText(state.getMessage().isEmpty() ? " " : state.getMessage());
        for (RecipeSearchResultViewData result : state.getResults()) {
            final JPanel row = new JPanel(new BorderLayout(8, 8));
            final RecipeImageView image = new RecipeImageView(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            row.add(image, BorderLayout.WEST);
            row.add(new JLabel(result.getName()), BorderLayout.CENTER);
            results.add(row);
            image.setImage(toUri(result.getThumbnailUrl()));
        }
        results.revalidate();
        results.repaint();
    }

    private static URI toUri(String url) {
        if (url.isEmpty()) {
            return null;
        }
        try {
            return URI.create(url);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
