package api.m2.movements.mappers;

import api.m2.movements.entities.Goal;
import api.m2.movements.entities.commons.Currency;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.currencies.CurrencyRecord;
import api.m2.movements.records.goals.GoalRecord;
import api.m2.movements.records.goals.GoalToAdd;
import api.m2.movements.repositories.CurrencyRepository;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GoalMapper {

    BigDecimal HUNDRED = new BigDecimal("100");

    @Mapping(target = "currency", source = "goalToAdd.currency", qualifiedByName = "mapCurrency")
    @Mapping(target = "currentAmount", ignore = true)
    Goal toEntity(GoalToAdd goalToAdd, @Context CurrencyRepository currencyRepository);

    @Named("mapCurrency")
    default Currency mapCurrency(String symbol, @Context CurrencyRepository currencyRepository) {
        if (symbol == null) {
            return null;
        }
        return currencyRepository.findBySymbol(symbol)
                .orElseThrow(() -> new EntityNotFoundException("Moneda no encontrada: " + symbol));
    }

    default GoalRecord toRecord(Goal goal) {
        if (goal == null) {
            return null;
        }
        return new GoalRecord(
                goal.getId(),
                goal.getWorkspaceId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                new CurrencyRecord(goal.getCurrency().getSymbol(), goal.getCurrency().getId()),
                goal.getTargetDate(),
                this.computeProgressPercent(goal),
                goal.getCreatedAt()
        );
    }

    default BigDecimal computeProgressPercent(Goal goal) {
        if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percent = goal.getCurrentAmount()
                .multiply(HUNDRED)
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        return percent.min(HUNDRED);
    }
}
