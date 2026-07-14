package api.m2.movements.movements.mappers;

import api.m2.movements.movements.entities.commons.Currency;
import api.m2.movements.movements.entities.movements.Subscription;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.movements.records.services.SubscriptionRecord;
import api.m2.movements.movements.records.services.SubscriptionToAdd;
import api.m2.movements.movements.records.services.UpdateSubscriptionRecord;
import api.m2.movements.movements.repositories.CurrencyRepository;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {
    @Mapping(target = "currency", source = "currency.symbol", qualifiedByName = "mapCurrency")
    @Mapping(target = "lastPayment", expression = "java(subscriptionToAdd.isPaid() != null "
            + "&& subscriptionToAdd.isPaid() ? subscriptionToAdd.lastPayment() : null)")
    Subscription toEntity(SubscriptionToAdd subscriptionToAdd, @Context CurrencyRepository currencyRepository);

    @Mapping(target = "currency.symbol", source = "currency.symbol")
    @Mapping(target = "isPaid", expression = "java(subscription.getIsPaid())")
    @Mapping(target = "workspaceName", ignore = true)
    @Mapping(target = "user", ignore = true)
    SubscriptionRecord toRecord(Subscription subscription);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "workspaceId", ignore = true)
    void updateMovement(UpdateSubscriptionRecord changesToMovement, @MappingTarget Subscription subscription);

    @Named("mapCurrency")
    default Currency mapCurrency(String symbol, @Context CurrencyRepository currencyRepository) {
        if (symbol == null) {
            return null;
        }
        return currencyRepository.findBySymbol(symbol)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found: " + symbol));
    }
}

