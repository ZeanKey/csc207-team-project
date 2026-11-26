package interface_adapter.search_recipes;

/**
 * What the Recipes page shows for one search result row.
 */
public final class RecipeSearchResultViewData {
    private final String id;
    private final String name;
    private final String thumbnailUrl;

    /**
     * Creates the view data for one result row.
     *
     * @param id the recipe identifier, used to open its details
     * @param name the recipe name to show
     * @param thumbnailUrl the picture address, or an empty string if none
     */
    public RecipeSearchResultViewData(String id, String name, String thumbnailUrl) {
        this.id = id;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl == null ? "" : thumbnailUrl;
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
     * Returns the picture address.
     *
     * @return the address, or an empty string if there is no picture
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
