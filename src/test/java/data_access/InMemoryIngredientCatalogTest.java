package data_access;

import java.util.Arrays;
import java.util.List;

import entity.IngredientName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryIngredientCatalogTest {

    @Test
    void findsKnownIngredientUsingNormalizedName() {
        InMemoryIngredientCatalog catalog = catalog("Chicken Breast", "Garlic");

        IngredientName result = catalog.find(
                IngredientName.fromRawName("  chicken_breast ")).orElseThrow();

        assertEquals("Chicken Breast", result.getDisplayName());
    }

    @Test
    void returnsEmptyForUnknownIngredient() {
        InMemoryIngredientCatalog catalog = catalog("Chicken Breast", "Garlic");

        assertFalse(catalog.find(IngredientName.fromRawName("Dragon Fruit")).isPresent());
    }

    @Test
    void suggestsNormalizedPrefixMatchesInAlphabeticalOrder() {
        InMemoryIngredientCatalog catalog = catalog(
                "Chicken Thigh", "Chickpeas", "Chicken Breast", "Garlic");

        List<String> suggestions = catalog.suggest("  CHICKEN_").stream()
                .map(IngredientName::getDisplayName)
                .toList();

        assertEquals(List.of("Chicken Breast", "Chicken Thigh"), suggestions);
    }

    @Test
    void returnsNoSuggestionsForBlankOrMissingQuery() {
        InMemoryIngredientCatalog catalog = catalog("Garlic");

        assertTrue(catalog.suggest(null).isEmpty());
        assertTrue(catalog.suggest("  ").isEmpty());
    }

    @Test
    void keepsFirstDisplayNameForNormalizedDuplicates() {
        InMemoryIngredientCatalog catalog = catalog("Chicken Breast", "chicken_breast");

        IngredientName result = catalog.find(
                IngredientName.fromRawName("CHICKEN BREAST")).orElseThrow();

        assertEquals("Chicken Breast", result.getDisplayName());
    }

    @Test
    void rejectsNullCatalogEntries() {
        assertThrows(NullPointerException.class,
                () -> new InMemoryIngredientCatalog(null));
        assertThrows(NullPointerException.class,
                () -> new InMemoryIngredientCatalog(Arrays.asList(
                        IngredientName.fromRawName("Garlic"), null)));
    }

    private static InMemoryIngredientCatalog catalog(String... names) {
        return new InMemoryIngredientCatalog(
                Arrays.stream(names)
                        .map(IngredientName::fromRawName)
                        .toList());
    }
}
