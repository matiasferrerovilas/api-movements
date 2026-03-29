package api.m2.movements.mappers;

import api.m2.movements.entities.Currency;
import api.m2.movements.entities.Subscription;
import api.m2.movements.records.services.SubscriptionRecord;
import api.m2.movements.records.services.SubscriptionToAdd;
import api.m2.movements.records.services.UpdateSubscriptionRecord;
import api.m2.movements.repositories.CurrencyRepository;
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
    @Mapping(target = "group", expression = "java(subscription.getAccount().getName())")
    @Mapping(target = "accountId", expression = "java(subscription.getAccount().getId())")
    @Mapping(target = "user", source = "owner.email")
    SubscriptionRecord toRecord(Subscription subscription);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "account", ignore = true)
    void updateMovement(UpdateSubscriptionRecord changesToMovement, @MappingTarget Subscription subscription);

    @Named("mapCurrency")
    default Currency mapCurrency(String symbol, @Context CurrencyRepository currencyRepository) {
        if (symbol == null) {
            return null;
        }
        return currencyRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Currency not found: " + symbol));
    }
}

