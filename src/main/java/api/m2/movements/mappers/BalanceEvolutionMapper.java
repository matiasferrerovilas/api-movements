package api.m2.movements.mappers;

import api.m2.movements.projections.MonthlyEvolutionProjection;
import api.m2.movements.records.balance.BalanceMonthlyEvolutionRecord;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mapper(componentModel = "spring")
public interface BalanceEvolutionMapper {

    int TOTAL_MONTHS = 12;

    BalanceMonthlyEvolutionRecord toRecord(MonthlyEvolutionProjection projection);

    List<BalanceMonthlyEvolutionRecord> toRecords(List<MonthlyEvolutionProjection> projections);

    default List<BalanceMonthlyEvolutionRecord> toRecordsWithFilledMonths(
            List<MonthlyEvolutionProjection> projections) {

        Map<String, Map<Integer, BigDecimal>> byCurrency = projections.stream()
                .collect(Collectors.groupingBy(
                        MonthlyEvolutionProjection::getCurrencySymbol,
                        Collectors.toMap(
                                MonthlyEvolutionProjection::getMonth,
                                MonthlyEvolutionProjection::getTotal
                        )
                ));

        return byCurrency.entrySet().stream()
                .flatMap(entry -> IntStream.rangeClosed(1, TOTAL_MONTHS)
                        .mapToObj(month -> new BalanceMonthlyEvolutionRecord(
                                month,
                                entry.getKey(),
                                entry.getValue().getOrDefault(month, BigDecimal.ZERO)
                        ))
                )
                .sorted(Comparator.comparing(BalanceMonthlyEvolutionRecord::month))
                .toList();
    }
}