package use_case.search_recipes;

/** The search text and search type supplied by the user. */
public final class SearchRecipesInputData {
    private final String keyword;
    private final RecipeSearchType searchType;

    public SearchRecipesInputData(String keyword, RecipeSearchType searchType) {
        this.keyword = keyword;
        this.searchType = searchType;
    }

    public String getKeyword() {
        return keyword;
    }

    public RecipeSearchType getSearchType() {
        return searchType;
    }
}
