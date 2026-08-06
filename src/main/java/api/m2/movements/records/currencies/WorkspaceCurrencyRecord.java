package api.m2.movements.records.currencies;

public record WorkspaceCurrencyRecord(
        Long id,
        String symbol,
        String description,
        boolean isDeletable
) {
}
