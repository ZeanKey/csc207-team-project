package data_access;

import entity.CookingRecord;
import entity.IngredientName;
import entity.MealPlan;
import entity.MealSlot;
import entity.MealType;
import entity.Pantry;
import entity.PantryItem;
import entity.PlannedMeal;
import entity.RecipeIngredient;
import entity.StockLevel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.complete_meal.CompleteMealDataAccessInterface;
import use_case.load_cooking_history.LoadCookingHistoryInputBoundary;
import use_case.load_uncooked_meals.LoadUncookedMealsInputBoundary;
import use_case.prepare_checklist.PrepareChecklistInputBoundary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Stores the standalone Cooking demo's plan, fridge, and history together. */
public final class FileCookingDataAccessObject implements CompleteMealDataAccessInterface,
        LoadUncookedMealsInputBoundary.DataAccess, PrepareChecklistInputBoundary.DataAccess,
        LoadCookingHistoryInputBoundary.DataAccess {
    private final Path file;
    private MealPlan mealPlan;
    private Pantry pantry;
    private List<CookingRecord> history = List.of();

    public FileCookingDataAccessObject(Path file, MealPlan samplePlan, Pantry samplePantry) {
        this.file = file.toAbsolutePath();
        mealPlan = samplePlan;
        pantry = samplePantry;
        if (!Files.notExists(this.file)) {
            loadFile(samplePlan.getWeekStart());
        }
    }

    @Override
    public synchronized MealPlan loadMealPlan(LocalDate weekStart) {
        return mealPlan.getWeekStart().equals(weekStart) ? mealPlan : null;
    }

    @Override
    public synchronized Pantry loadPantry() {
        return pantry;
    }

    @Override
    public synchronized List<CookingRecord> loadHistory() {
        return history;
    }

    @Override
    public synchronized Snapshot loadSnapshot(LocalDate weekStart) {
        return new Snapshot(loadMealPlan(weekStart), pantry);
    }

    @Override
    public synchronized boolean saveCompletion(Snapshot original, MealPlan updatedPlan,
                                               Pantry updatedPantry, CookingRecord record) {
        if (original.mealPlan() != mealPlan || original.pantry() != pantry
                || history.stream().anyMatch(entry -> entry.weekStart().equals(record.weekStart())
                        && entry.slot().equals(record.slot()))) {
            return false;
        }

        final List<CookingRecord> records = new ArrayList<>(history);
        records.add(record);
        final JSONObject data = new JSONObject()
                .put("plan", writePlan(updatedPlan))
                .put("pantry", writePantry(updatedPantry))
                .put("history", writeHistory(records));
        writeFile(data);
        mealPlan = updatedPlan;
        pantry = updatedPantry;
        history = List.copyOf(records);
        return true;
    }

    private void loadFile(LocalDate currentWeek) {
        try {
            final JSONObject data = new JSONObject(Files.readString(file));
            final JSONObject plan = data.getJSONObject("plan");
            if (LocalDate.parse(plan.getString("week")).equals(currentWeek)) {
                mealPlan = readPlan(plan, currentWeek);
            }
            pantry = readPantry(data.getJSONArray("pantry"));
            history = readHistory(data.getJSONArray("history"));
        }
        catch (IOException | JSONException | DateTimeParseException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load Cooking demo data.", exception);
        }
    }

    private void writeFile(JSONObject data) {
        try {
            final Path folder = Files.createDirectories(file.getParent());
            final Path temporary = Files.createTempFile(folder, "cooking-", ".tmp");
            try {
                Files.writeString(temporary, data.toString(2));
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException exception) {
                try {
                    Files.deleteIfExists(temporary);
                }
                catch (IOException cleanupError) {
                    exception.addSuppressed(cleanupError);
                }
                throw exception;
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not save Cooking demo data.", exception);
        }
    }

    private MealPlan readPlan(JSONObject data, LocalDate week) {
        final MealPlan plan = new MealPlan(week);
        final JSONArray meals = data.getJSONArray("meals");
        for (int index = 0; index < meals.length(); index++) {
            final JSONObject entry = meals.getJSONObject(index);
            final List<RecipeIngredient> ingredients = new ArrayList<>();
            final JSONArray items = entry.getJSONArray("ingredients");
            for (int item = 0; item < items.length(); item++) {
                final JSONObject ingredient = items.getJSONObject(item);
                ingredients.add(new RecipeIngredient(
                        IngredientName.fromRawName(ingredient.getString("name")),
                        ingredient.optString("measure", "")));
            }
            final PlannedMeal meal = new PlannedMeal(
                    entry.getString("recipeId"), entry.getString("recipeName"), ingredients);
            if (entry.getBoolean("cooked")) {
                meal.markCooked();
            }
            plan.assign(readSlot(entry), meal);
        }
        return plan;
    }

    private JSONObject writePlan(MealPlan plan) {
        final JSONArray meals = new JSONArray();
        plan.getMeals().forEach((slot, meal) -> {
            final JSONArray ingredients = new JSONArray();
            for (RecipeIngredient ingredient : meal.getIngredients()) {
                ingredients.put(new JSONObject()
                        .put("name", ingredient.getName().getDisplayName())
                        .put("measure", ingredient.getMeasureDescription()));
            }
            meals.put(writeSlot(slot)
                    .put("recipeId", meal.getRecipeId())
                    .put("recipeName", meal.getRecipeName())
                    .put("cooked", meal.isCooked())
                    .put("ingredients", ingredients));
        });
        return new JSONObject().put("week", plan.getWeekStart().toString()).put("meals", meals);
    }

    private Pantry readPantry(JSONArray items) {
        final Pantry result = new Pantry();
        for (int index = 0; index < items.length(); index++) {
            final JSONObject item = items.getJSONObject(index);
            final LocalDate expiry = item.isNull("expiryDate")
                    ? null : LocalDate.parse(item.getString("expiryDate"));
            result.add(new PantryItem(IngredientName.fromRawName(item.getString("name")),
                    expiry, StockLevel.valueOf(item.getString("stockLevel"))));
        }
        return result;
    }

    private JSONArray writePantry(Pantry updatedPantry) {
        final JSONArray items = new JSONArray();
        for (PantryItem item : updatedPantry.sortedByExpiry()) {
            final LocalDate expiry = item.getExpiryDate();
            items.put(new JSONObject()
                    .put("name", item.getName().getDisplayName())
                    .put("expiryDate", expiry == null ? JSONObject.NULL : expiry.toString())
                    .put("stockLevel", item.getStockLevel().name()));
        }
        return items;
    }

    private List<CookingRecord> readHistory(JSONArray records) {
        final List<CookingRecord> result = new ArrayList<>();
        for (int index = 0; index < records.length(); index++) {
            final JSONObject record = records.getJSONObject(index);
            result.add(new CookingRecord(record.getString("recipeId"), record.getString("recipeName"),
                    LocalDate.parse(record.getString("cookedOn")),
                    LocalDate.parse(record.getString("week")), readSlot(record)));
        }
        return List.copyOf(result);
    }

    private JSONArray writeHistory(List<CookingRecord> records) {
        final JSONArray items = new JSONArray();
        for (CookingRecord record : records) {
            items.put(writeSlot(record.slot())
                    .put("recipeId", record.recipeId())
                    .put("recipeName", record.recipeName())
                    .put("cookedOn", record.cookedOn().toString())
                    .put("week", record.weekStart().toString()));
        }
        return items;
    }

    private MealSlot readSlot(JSONObject data) {
        return new MealSlot(DayOfWeek.valueOf(data.getString("day")),
                MealType.valueOf(data.getString("mealType")));
    }

    private JSONObject writeSlot(MealSlot slot) {
        return new JSONObject()
                .put("day", slot.getDay().name())
                .put("mealType", slot.getMealType().name());
    }
}
