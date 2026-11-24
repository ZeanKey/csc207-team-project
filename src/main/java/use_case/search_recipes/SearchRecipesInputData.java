package use_case.search_recipes;

/**
 * The search text and search type supplied by the user.
 */
public final class SearchRecipesInputData {
    private final String keyword;
    private final RecipeSearchType searchType;

    /**
     * Creates a search request. Validation is performed by the interactor.
     *
     * @param keyword the user-entered text; may be null
     * @param searchType the search mode; may be null
     */
    public SearchRecipesInputData(String keyword, RecipeSearchType searchType) {
        this.keyword = keyword;
        this.searchType = searchType;
    }

    /**
     * Returns the user-entered search text.
     *
     * @return the search text, or null if none was supplied
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns the requested search mode.
     *
     * @return the search mode, or null if none was supplied
     */
    public RecipeSearchType getSearchType() {
        return searchType;
    }
}
