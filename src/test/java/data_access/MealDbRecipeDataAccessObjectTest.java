package data_access;

import entity.Recipe;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealDbRecipeDataAccessObjectTest {
    private static final String FULL = """
            {"meals":[{"idMeal":"52771","strMeal":"Pasta", "strCategory":"Vegetarian",
            "strArea":"Italian", "strInstructions":"Cook", "strMealThumb":"https://example.com/a.jpg",
            "strIngredient1":"Garlic", "strMeasure1":"2 cloves", "strIngredient2":"",
            "strIngredient3":null, "strIngredient4":"Pepper", "strMeasure4":null}]}
            """;

    @Test
    void nameSearchEncodesKeywordAndMapsFullRecipe() throws IOException {
        List<URI> requests = new ArrayList<>();
        MealDbRecipeDataAccessObject dao = new MealDbRecipeDataAccessObject(uri -> {
            requests.add(uri);
            return FULL;
        });

        Recipe recipe = dao.searchByName("pasta & sauce").get(0);

        assertTrue(requests.get(0).toString().endsWith("search.php?s=pasta+%26+sauce"));
        assertEquals("meal:52771", recipe.getId());
        assertEquals("Pasta", recipe.getName());
        assertEquals("Cook", recipe.getInstruction());
        assertEquals(URI.create("https://example.com/a.jpg"), recipe.getThumbnail());
        assertEquals(2, recipe.getIngredients().size());
        assertEquals("Garlic", recipe.getIngredients().get(0).getDisplayName());
        assertEquals("2 cloves", recipe.getIngredients().get(0).getMeasureDescription());
        assertEquals("", recipe.getIngredients().get(1).getMeasureDescription());
    }

    @Test
    void ingredientSearchLoadsFullDetailsForEachMatch() throws IOException {
        List<URI> requests = new ArrayList<>();
        MealDbRecipeDataAccessObject dao = new MealDbRecipeDataAccessObject(uri -> {
            requests.add(uri);
            return uri.getPath().endsWith("filter.php")
                    ? "{\"meals\":[{\"idMeal\":\"52771\",\"strMeal\":\"Pasta\"}]}" : FULL;
        });

        Recipe recipe = dao.searchByIngredient("chicken breast").get(0);

        assertTrue(requests.get(0).toString().endsWith("filter.php?i=chicken_breast"));
        assertTrue(requests.get(1).toString().endsWith("lookup.php?i=52771"));
        assertEquals("Cook", recipe.getInstruction());
        assertEquals(2, recipe.getIngredients().size());
    }

    @Test
    void noResultsAndOptionalFieldsAreHandled() throws IOException {
        MealDbRecipeDataAccessObject empty = new MealDbRecipeDataAccessObject(uri -> "{\"meals\":null}");
        assertTrue(empty.searchByName("unknown").isEmpty());
        assertTrue(empty.searchByIngredient("unknown").isEmpty());

        MealDbRecipeDataAccessObject missing = new MealDbRecipeDataAccessObject(uri ->
                "{\"meals\":[{\"idMeal\":\"1\",\"strMeal\":\"Soup\",\"strMealThumb\":null,"
                        + "\"strInstructions\":null,\"strIngredient1\":null,\"strMeasure1\":null}]}");
        Recipe recipe = missing.searchByName("soup").get(0);
        assertNull(recipe.getThumbnail());
        assertNull(recipe.getArea());
        assertTrue(recipe.getIngredients().isEmpty());
    }

    @Test
    void failureToLoadOneDetailDoesNotReturnPartialResults() {
        MealDbRecipeDataAccessObject dao = new MealDbRecipeDataAccessObject(uri ->
                uri.getPath().endsWith("filter.php")
                        ? "{\"meals\":[{\"idMeal\":\"52771\"},{\"idMeal\":\"12345\"}]}"
                        : uri.getQuery().equals("i=52771") ? FULL : "{\"meals\":null}");
        assertThrows(IOException.class, () -> dao.searchByIngredient("garlic"));
    }

    @Test
    void transportFailureIsPropagated() {
        MealDbRecipeDataAccessObject dao = new MealDbRecipeDataAccessObject(uri -> {
            throw new IOException("network unavailable");
        });
        IOException exception = assertThrows(IOException.class, () -> dao.searchByName("soup"));
        assertEquals("network unavailable", exception.getMessage());
    }

    @Test
    void malformedResponseIsReportedAsFailure() {
        MealDbRecipeDataAccessObject dao = new MealDbRecipeDataAccessObject(uri -> "not json");
        assertThrows(IOException.class, () -> dao.searchByName("soup"));
    }

    @Test
    void partialRecipeCannotBeMistakenForACompleteSnapshot() {
        MealDbRecipeDataAccessObject dao = new MealDbRecipeDataAccessObject(uri ->
                "{\"meals\":[{\"idMeal\":\"1\",\"strMeal\":\"Soup\"}]}");
        assertThrows(IOException.class, () -> dao.searchByName("soup"));
    }
}
