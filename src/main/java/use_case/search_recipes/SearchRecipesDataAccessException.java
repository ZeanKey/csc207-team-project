package use_case.search_recipes;

/** Indicates that the recipe source could not complete a search. */
public class SearchRecipesDataAccessException extends RuntimeException {
    public SearchRecipesDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public SearchRecipesDataAccessException(String message) {
        super(message);
    }
}
