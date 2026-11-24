package data_access;

import entity.IngredientName;
import entity.Recipe;
import entity.RecipeIngredient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.search_recipes.SearchRecipesDataAccessInterface;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
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
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .callTimeout(Duration.ofSeconds(15))
                .build();
        this.transport = uri -> {
            Request request = new Request.Builder().url(uri.toString()).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 200) {
                    throw new IOException("Recipe service returned HTTP " + response.code());
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new IOException("Recipe service returned an empty response body");
                }
                return body.string();
            }
        };
    }

    /**
     * Creates a DAO with a transport, allowing tests to avoid network access.
     *
     * @param transport the request transport
     * @throws NullPointerException if transport is null
     */
    MealDbRecipeDataAccessObject(Transport transport) {
        this.transport = Objects.requireNonNull(transport);
    }

    @FunctionalInterface
    interface Transport {
        /**
         * Retrieves the response body for a request.
         *
         * @param uri the request URI
         * @return the response body
         * @throws IOException if the request cannot be completed
         */
        String get(URI uri) throws IOException;
    }

    /**
     * Searches TheMealDB for complete recipes by name.
     *
     * @param keyword non-null recipe name or search text
     * @return complete recipe snapshots, or an empty list if none match
     * @throws IOException if the request fails, is interrupted, or the response cannot be converted
     * @throws NullPointerException if keyword is null
     */
    @Override
    public List<Recipe> searchByName(String keyword) throws IOException {
        JSONArray meals = getMeals("search.php?s=" + encode(keyword));
        List<Recipe> recipes = new ArrayList<>();
        try {
            for (int i = 0; i < meals.length(); i++) {
                recipes.add(toRecipe(meals.getJSONObject(i)));
            }
            return recipes;
        }
        catch (JSONException | IllegalArgumentException exception) {
            throw new IOException("Invalid recipe data", exception);
        }
    }

    /**
     * Searches TheMealDB by ingredient and loads details for each match.
     *
     * @param keyword non-null ingredient name
     * @return complete recipe snapshots, or an empty list if none match
     * @throws IOException if a request fails or is interrupted, or a response is invalid
     * @throws NullPointerException if keyword is null
     */
    @Override
    public List<Recipe> searchByIngredient(String keyword) throws IOException {
        String ingredient = keyword.strip().replaceAll("\\s+", "_");
        JSONArray matches = getMeals("filter.php?i=" + encode(ingredient));
        List<Recipe> recipes = new ArrayList<>();
        try {
            for (int i = 0; i < matches.length(); i++) {
                String id = required(matches.getJSONObject(i), "idMeal");
                JSONArray details = getMeals("lookup.php?i=" + encode(id));
                if (details.length() != 1) {
                    throw new IOException("A matching recipe could not be loaded");
                }
                JSONObject meal = details.getJSONObject(0);
                if (!id.equals(required(meal, "idMeal"))) {
                    throw new IOException("Recipe lookup returned an unexpected ID");
                }
                recipes.add(toRecipe(meal));
            }
            return recipes;
        }
        catch (JSONException | IllegalArgumentException exception) {
            throw new IOException("Invalid recipe data", exception);
        }
    }

    private JSONArray getMeals(String endpoint) throws IOException {
        String response = transport.get(URI.create(API + endpoint));
        try {
            JSONObject root = new JSONObject(response);
            if (!root.has("meals")) {
                throw new IOException("Recipe response is missing meals");
            }
            return root.isNull("meals") ? new JSONArray() : root.getJSONArray("meals");
        }
        catch (JSONException exception) {
            throw new IOException("Invalid recipe response", exception);
        }
    }

    private static Recipe toRecipe(JSONObject meal) throws IOException {
        if (!meal.has("strInstructions") || !meal.has("strIngredient1") || !meal.has("strMeasure1")) {
            throw new IOException("Recipe response does not contain full details");
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

    private static String required(JSONObject json, String key) throws IOException {
        String value = optional(json, key);
        if (value == null || value.isBlank()) {
            throw new IOException("Recipe response is missing " + key);
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
