package data_access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import entity.IngredientName;
import use_case.ingredient_catalog.IngredientCatalog;

/**
 * Stores a set of known ingredient names in memory.
 */
public final class InMemoryIngredientCatalog implements IngredientCatalog {
    private final Map<IngredientName, IngredientName> ingredients = new LinkedHashMap<>();

    /**
     * Creates a catalog from names supplied by a loader or test.
     *
     * @param knownIngredients the known ingredient names
     */
    public InMemoryIngredientCatalog(Collection<IngredientName> knownIngredients) {
        Objects.requireNonNull(knownIngredients, "known ingredients cannot be null");
        for (IngredientName ingredient : knownIngredients) {
            final IngredientName nonNullIngredient = Objects.requireNonNull(
                    ingredient, "known ingredient cannot be null");
            ingredients.putIfAbsent(nonNullIngredient, nonNullIngredient);
        }
    }

    @Override
    public Optional<IngredientName> find(IngredientName name) {
        return Optional.ofNullable(ingredients.get(name));
    }

    @Override
    public List<IngredientName> suggest(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        final IngredientName normalizedQuery;
        try {
            normalizedQuery = IngredientName.fromRawName(query);
        }
        catch (IllegalArgumentException exception) {
            return List.of();
        }

        final List<IngredientName> suggestions = new ArrayList<>();
        for (IngredientName ingredient : ingredients.values()) {
            if (ingredient.getKey().startsWith(normalizedQuery.getKey())) {
                suggestions.add(ingredient);
            }
        }
        suggestions.sort(Comparator.comparing(IngredientName::getKey));
        return List.copyOf(suggestions);
    }
}
