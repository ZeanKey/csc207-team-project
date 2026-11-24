package use_case.remove_pantry_item;

/**
 * Reports the removed ingredient's display name or a message on failure.
 */
public final class RemovePantryItemOutputData {
    private final String ingredientName;
    private final String message;
    private final boolean useCaseFailed;

    public RemovePantryItemOutputData(String ingredientName, String message, boolean useCaseFailed) {
        this.ingredientName = ingredientName;
        this.message = message;
        this.useCaseFailed = useCaseFailed;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUseCaseFailed() {
        return useCaseFailed;
    }
}
