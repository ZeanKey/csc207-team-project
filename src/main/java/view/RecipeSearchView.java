package view;

import entity.Recipe;
import interface_adapter.search_recipes.SearchRecipesController;
import interface_adapter.search_recipes.SearchRecipesViewModel;
import use_case.search_recipes.RecipeSearchType;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/** Search form and result list, usable inside an application or a standalone demo. */
public final class RecipeSearchView extends JPanel {
    private final SearchRecipesViewModel viewModel;
    private final SearchRecipesController controller;
    private final JTextField keyword = new JTextField(20);
    private final JComboBox<RecipeSearchType> searchType = new JComboBox<>(RecipeSearchType.values());
    private final JButton search = new JButton("Search");
    private final JLabel message = new JLabel(" ");
    private final JPanel results = new JPanel();

    public RecipeSearchView(SearchRecipesViewModel viewModel, SearchRecipesController controller) {
        super(new BorderLayout(8, 8));
        this.viewModel = Objects.requireNonNull(viewModel);
        this.controller = Objects.requireNonNull(controller);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel controls = new JPanel();
        controls.add(new JLabel("Search recipes:"));
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
        message.setText("Searching...");
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
                    message.setText("Search interrupted. Please try again.");
                }
                catch (ExecutionException exception) {
                    message.setText("Search failed. Please try again.");
                }
            }
        }.execute();
    }

    private void refresh() {
        results.removeAll();
        message.setText(viewModel.getMessage().isEmpty() ? " " : viewModel.getMessage());
        for (Recipe recipe : viewModel.getRecipes()) {
            JPanel row = new JPanel(new BorderLayout(8, 8));
            JLabel image = new JLabel("No image", JLabel.CENTER);
            image.setPreferredSize(new Dimension(96, 72));
            row.add(image, BorderLayout.WEST);
            row.add(new JLabel(recipe.getName()), BorderLayout.CENTER);
            results.add(row);
            loadImage(recipe.getThumbnail(), image);
        }
        results.revalidate();
        results.repaint();
    }

    private static void loadImage(URI uri, JLabel label) {
        if (uri == null || !("https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme()) || "file".equalsIgnoreCase(uri.getScheme())
                || "jar".equalsIgnoreCase(uri.getScheme()))) {
            return;
        }
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws IOException {
                URLConnection connection = uri.toURL().openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                try (InputStream stream = connection.getInputStream()) {
                    BufferedImage image = ImageIO.read(stream);
                    return image == null ? null : new ImageIcon(image.getScaledInstance(96, 72, Image.SCALE_SMOOTH));
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        label.setText("");
                        label.setIcon(icon);
                    }
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException exception) {
                    // Keep the placeholder if the image is unavailable.
                }
            }
        }.execute();
    }
}
