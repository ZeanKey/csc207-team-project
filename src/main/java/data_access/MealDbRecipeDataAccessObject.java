package data_access;

import entity.IngredientName;
import entity.Recipe;
import entity.RecipeIngredient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.search_recipes.SearchRecipesDataAccessException;
import use_case.search_recipes.SearchRecipesDataAccessInterface;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads complete recipe snapshots from TheMealDB's v1 API.
 */
public final class MealDbRecipeDataAccessObject implements SearchRecipesDataAccessInterface {
    private static final String API = "https://www.themealdb.com/api/json/v1/1/";
    private static final String ID_PREFIX = "meal:";
    private final Transport transport;

    /**
     * Creates a DAO using the educational API key and a bounded HTTP timeout.
     */
    public MealDbRecipeDataAccessObject() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.transport = uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET().build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new SearchRecipesDataAccessException("Recipe service returned HTTP " + response.statusCode());
                }
                return response.body();
            }
            catch (IOException exception) {
                throw new SearchRecipesDataAccessException("Unable to reach the recipe service", exception);
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new SearchRecipesDataAccessException("Recipe search was interrupted", exception);
            }
        };
    }

    // Allows deterministic tests without network access.
    MealDbRecipeDataAccessObject(Transport transport) {
        this.transport = Objects.requireNonNull(transport);
    }

    @FunctionalInterface
    interface Transport {
        String get(URI uri);
    }

    @Override
    public List<Recipe> searchByName(String keyword) {
        JSONArray meals = getMeals("search.php?s=" + encode(keyword));
        List<Recipe> recipes = new ArrayList<>();
        try {
            for (int i = 0; i < meals.length(); i++) {
                recipes.add(toRecipe(meals.getJSONObject(i)));
            }
            return recipes;
        }
        catch (JSONException | IllegalArgumentException exception) {
            throw new SearchRecipesDataAccessException("Invalid recipe data", exception);
        }
    }

    @Override
    public List<Recipe> searchByIngredient(String keyword) {
        String ingredient = keyword.strip().replaceAll("\\s+", "_");
        JSONArray matches = getMeals("filter.php?i=" + encode(ingredient));
        List<Recipe> recipes = new ArrayList<>();
        try {
            for (int i = 0; i < matches.length(); i++) {
                String id = required(matches.getJSONObject(i), "idMeal");
                JSONArray details = getMeals("lookup.php?i=" + encode(id));
                if (details.length() != 1) {
                    throw new SearchRecipesDataAccessException("A matching recipe could not be loaded");
                }
                JSONObject meal = details.getJSONObject(0);
                if (!id.equals(required(meal, "idMeal"))) {
                    throw new SearchRecipesDataAccessException("Recipe lookup returned an unexpected ID");
                }
                recipes.add(toRecipe(meal));
            }
            return recipes;
        }
        catch (JSONException | IllegalArgumentException exception) {
            throw new SearchRecipesDataAccessException("Invalid recipe data", exception);
        }
    }

    private JSONArray getMeals(String endpoint) {
        String response = transport.get(URI.create(API + endpoint));
        try {
            JSONObject root = new JSONObject(response);
            if (!root.has("meals")) {
                throw new SearchRecipesDataAccessException("Recipe response is missing meals");
            }
            return root.isNull("meals") ? new JSONArray() : root.getJSONArray("meals");
        }
        catch (JSONException exception) {
            throw new SearchRecipesDataAccessException("Invalid recipe response", exception);
        }
    }

    private static Recipe toRecipe(JSONObject meal) {
        if (!meal.has("strInstructions") || !meal.has("strIngredient1") || !meal.has("strMeasure1")) {
            throw new SearchRecipesDataAccessException("Recipe response does not contain full details");
        }
        String id = required(meal, "idMeal");
        String name = required(meal, "strMeal");
        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String ingredient = optional(meal, "strIngredient" + i);
            if (ingredient != null && !ingredient.isBlank()) {
                String measure = optional(meal, "strMeasure" + i);
                ingredients.add(new RecipeIngredient(IngredientName.fromRawName(ingredient),
                        measure == null ? "" : measure.strip()));
            }
        }
        String thumbnail = optional(meal, "strMealThumb");
        return new Recipe(ID_PREFIX + id, name, optional(meal, "strCategory"),
                optional(meal, "strArea"), optional(meal, "strInstructions"),
                thumbnail == null || thumbnail.isBlank() ? null : URI.create(thumbnail), ingredients);
    }

    private static String required(JSONObject json, String key) {
        String value = optional(json, key);
        if (value == null || value.isBlank()) {
            throw new SearchRecipesDataAccessException("Recipe response is missing " + key);
        }
        return value;
    }

    private static String optional(JSONObject json, String key) {
        return json.isNull(key) ? null : json.getString(key);
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
