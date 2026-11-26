package interface_adapter.pantry;

import java.time.LocalDate;

import entity.StockLevel;

/**
 * Immutable data for one row in the fridge view.
 *
 * @param displayName the name shown to the user
 * @param ingredientKey the normalized ingredient identifier
 * @param expiryDate the expiry date, or {@code null} when none is set
 * @param stockLevel the current stock level
 * @param expired whether the item has expired
 * @param expiringSoon whether the item expires within three days
 */
public record PantryItemViewData(String displayName, String ingredientKey, LocalDate expiryDate,
                                 StockLevel stockLevel, boolean expired, boolean expiringSoon) {
}
