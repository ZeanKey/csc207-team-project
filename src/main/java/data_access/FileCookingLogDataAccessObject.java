package data_access;

import entity.CookingRecord;
import entity.MealSlot;
import entity.MealType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Reads and appends the shared cooking history. */
public final class FileCookingLogDataAccessObject implements SharedCookingDataAccessObject.HistoryStore {
    private final Path file;

    public FileCookingLogDataAccessObject() {
        this(Path.of("data", "cooking_log.json"));
    }

    public FileCookingLogDataAccessObject(Path file) {
        this.file = file.toAbsolutePath();
    }

    @Override
    public List<CookingRecord> loadHistory() {
        if (Files.notExists(file)) {
            return List.of();
        }
        try {
            return readRecords(new JSONArray(Files.readString(file)));
        }
        catch (IOException | JSONException | DateTimeParseException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not read cooking history.", exception);
        }
    }

    @Override
    public void append(CookingRecord record) {
        final List<CookingRecord> records = new ArrayList<>(loadHistory());
        records.add(record);
        try {
            replaceFile(file, writeRecords(records).toString(2));
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not save cooking history.", exception);
        }
    }

    static void replaceFile(Path file, String contents) throws IOException {
        final Path folder = Files.createDirectories(file.getParent());
        final Path temporary = Files.createTempFile(folder, "cooking-log-", ".tmp");
        try {
            Files.writeString(temporary, contents);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            }
            catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    static List<CookingRecord> readRecords(JSONArray records) {
        final List<CookingRecord> result = new ArrayList<>();
        for (int index = 0; index < records.length(); index++) {
            final JSONObject record = records.getJSONObject(index);
            result.add(new CookingRecord(record.getString("recipeId"), record.getString("recipeName"),
                    LocalDate.parse(record.getString("cookedOn")),
                    LocalDate.parse(record.getString("week")), readSlot(record)));
        }
        return List.copyOf(result);
    }

    static JSONArray writeRecords(List<CookingRecord> records) {
        final JSONArray result = new JSONArray();
        for (CookingRecord record : records) {
            result.put(writeSlot(record.slot())
                    .put("recipeId", record.recipeId())
                    .put("recipeName", record.recipeName())
                    .put("cookedOn", record.cookedOn().toString())
                    .put("week", record.weekStart().toString()));
        }
        return result;
    }

    static MealSlot readSlot(JSONObject data) {
        return new MealSlot(DayOfWeek.valueOf(data.getString("day")),
                MealType.valueOf(data.getString("mealType")));
    }

    static JSONObject writeSlot(MealSlot slot) {
        return new JSONObject()
                .put("day", slot.getDay().name())
                .put("mealType", slot.getMealType().name());
    }
}
