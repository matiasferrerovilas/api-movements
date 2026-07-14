package api.m2.movements.records.categories;

import jakarta.validation.constraints.NotNull;

public record CategoryMigrateRequest(
        @NotNull Long fromCategoryId,
        @NotNull Long toCategoryId
) {
}
