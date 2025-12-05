package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.entities.Services;
import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.ServiceToAdd;
import api.expenses.expenses.records.services.UpdateServiceRecord;
import api.expenses.expenses.repositories.CurrencyRepository;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceMapper {
    @Mapping(target = "currency", source = "currency.symbol", qualifiedByName = "mapCurrency")
    @Mapping(target = "lastPayment", expression = "java(serviceToAdd.isPaid() != null && serviceToAdd.isPaid() ? serviceToAdd.lastPayment() : null)")
    Services toEntity(ServiceToAdd serviceToAdd, @Context CurrencyRepository currencyRepository);

    @Mapping(target = "currency.symbol", source = "currency.symbol")
    @Mapping(target = "isPaid", expression = "java(services.getIsPaid())")
    @Mapping(target = "group", expression = "java(services.getUserGroups().getDescription())")
    @Mapping(target = "user", source = "users.email")
    ServiceRecord toRecord(Services services);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    void updateMovement(UpdateServiceRecord changesToMovement, @MappingTarget Services service);

    @Named("mapCurrency")
    default Currency mapCurrency(String symbol, @Context CurrencyRepository currencyRepository) {
        if (symbol == null) {
            return null;
        }
        return currencyRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Currency not found: " + symbol));
    }
}
