package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.entities.Services;
import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.ServiceToAdd;
import api.expenses.expenses.repositories.CurrencyRepository;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceMapper {
    @Mapping(target = "currency", source = "currency.symbol", qualifiedByName = "mapCurrency")
    @Mapping(target = "lastPayment", expression = "java(serviceToAdd.isPaid() != null && serviceToAdd.isPaid() ? java.time.LocalDate.now() : null)")
    Services toEntity(ServiceToAdd serviceToAdd, @Context CurrencyRepository currencyRepository);

    @Mapping(target = "currency.symbol", source = "currency.symbol")
    @Mapping(target = "isPaid", expression = "java(services.getIsPaid())")
    ServiceRecord toRecord(Services services);

    @Named("mapCurrency")
    default Currency mapCurrency(String symbol, @Context CurrencyRepository currencyRepository) {
        if (symbol == null) {
            return null;
        }
        return currencyRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Currency not found: " + symbol));
    }
}
