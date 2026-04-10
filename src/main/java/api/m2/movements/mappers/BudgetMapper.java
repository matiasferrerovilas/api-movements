package api.m2.movements.mappers;

import api.m2.movements.entities.Budget;
import api.m2.movements.entities.Category;
import api.m2.movements.entities.Currency;
import api.m2.movements.records.BudgetToAdd;
import api.m2.movements.records.budgets.BudgetRecord;
import api.m2.movements.repositories.CategoryRepository;
import api.m2.movements.repositories.CurrencyRepository;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BudgetMapper {

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategory")
    @Mapping(target = "currency", source = "currency", qualifiedByName = "mapCurrency")
    @Mapping(target = "account", ignore = true)
    Budget toEntity(BudgetToAdd budgetToAdd,
                    @Context CategoryRepository categoryRepository,
                    @Context CurrencyRepository currencyRepository);

    @Mapping(target = "accountId", expression = "java(budget.getAccount().getId())")
    @Mapping(target = "spent", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "percentage", expression = "java(java.math.BigDecimal.ZERO)")
    BudgetRecord toRecord(Budget budget);

    @Named("mapCategory")
    default Category mapCategory(String categoryName, @Context CategoryRepository categoryRepository) {
        if (categoryName == null) {
            return null;
        }
        return categoryRepository.findByDescription(categoryName)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));
    }

    @Named("mapCurrency")
    default Currency mapCurrency(String symbol, @Context CurrencyRepository currencyRepository) {
        if (symbol == null) {
            return null;
        }
        return currencyRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Currency not found: " + symbol));
    }

    default BudgetRecord toRecordWithSpent(Budget budget, BigDecimal spent) {
        BigDecimal percentage = budget.getAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : spent.multiply(new BigDecimal("100")).divide(budget.getAmount(), 2, java.math.RoundingMode.HALF_UP);
        return new BudgetRecord(
                budget.getId(),
                budget.getAccount().getId(),
                budget.getCategory() == null ? null : new api.m2.movements.records.categories.CategoryRecord(
                        budget.getCategory().getId(),
                        budget.getCategory().getDescription(),
                        true,
                        budget.getCategory().isDeletable()),
                new api.m2.movements.records.currencies.CurrencyRecord(
                        budget.getCurrency().getSymbol(),
                        budget.getCurrency().getId()),
                budget.getAmount(),
                budget.getYear(),
                budget.getMonth(),
                spent,
                percentage
        );
    }
}
