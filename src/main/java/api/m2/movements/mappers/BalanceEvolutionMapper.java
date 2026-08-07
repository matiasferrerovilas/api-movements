package api.m2.movements.mappers;

import api.m2.movements.projections.MonthlyEvolutionProjection;
import api.m2.movements.records.balance.BalanceMonthlyEvolutionRecord;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mapper(componentModel = "spring")
public interface BalanceEvolutionMapper {

    int TOTAL_MONTHS = 12;

    default List<BalanceMonthlyEvolutionRecord> toRecordsWithFilledMonths(
            List<MonthlyEvolutionProjection> projections) {

        Map<String, Map<Integer, MonthlyEvolutionProjection>> byCurrency = projections.stream()
                .collect(Collectors.groupingBy(
                        MonthlyEvolutionProjection::getCurrencySymbol,
                        Collectors.toMap(
                                MonthlyEvolutionProjection::getMonth,
                                Function.identity()
                        )
                ));

        return byCurrency.entrySet().stream()
                .flatMap(entry -> IntStream.rangeClosed(1, TOTAL_MONTHS)
                        .mapToObj(month -> {
                            var projection = entry.getValue().get(month);
                            var spent = projection != null ? projection.getSpent() : BigDecimal.ZERO;
                            var income = projection != null ? projection.getIncome() : BigDecimal.ZERO;
                            return new BalanceMonthlyEvolutionRecord(
                                    month,
                                    entry.getKey(),
                                    spent,
                                    income.subtract(spent)
                            );
                        })
                )
                .sorted(Comparator.comparing(BalanceMonthlyEvolutionRecord::month))
                .toList();
    }
}