package use_case.search_recipes;

import entity.Recipe;
import java.util.List;

/** Searches a recipe source and returns complete snapshots. */
public interface SearchRecipesDataAccessInterface {
    /** Returns an empty list when nothing matches; throws on access or conversion failure. */
    List<Recipe> searchByName(String keyword) throws SearchRecipesDataAccessException;

    /** Returns an empty list when nothing matches; throws if any result cannot be loaded. */
    List<Recipe> searchByIngredient(String keyword) throws SearchRecipesDataAccessException;
}
