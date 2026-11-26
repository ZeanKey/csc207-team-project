package use_case.search_recipes;

import java.net.URI;
import java.util.Objects;

/**
 * What a search result needs to show about one matching recipe.
 *
 * <p>Plain data copied out of the Recipe entity, so that the Presenter and
 * the View never depend on the entity itself.</p>
 */
public final class RecipeSummaryOutputData {
    private final String id;
    private final String name;
    private final URI thumbnail;

    /**
     * Creates the summary of one matching recipe.
     *
     * @param id the recipe identifier, used later to open its details
     * @param name the recipe name
     * @param thumbnail the picture location, or null if there is none
     * @throws NullPointerException if id or name is null
     */
    public RecipeSummaryOutputData(String id, String name, URI thumbnail) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.thumbnail = thumbnail;
    }

    /**
     * Returns the recipe identifier.
     *
     * @return the identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the recipe name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the picture location.
     *
     * @return the location, or null if there is none
     */
    public URI getThumbnail() {
        return thumbnail;
    }
}
