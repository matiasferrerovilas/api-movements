package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.entities.Services;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.ServiceToAdd;
import api.expenses.expenses.repositories.CurrencyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-23T17:56:34-0300",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.2.1.jar, environment: Java 25 (Amazon.com Inc.)"
)
@Component
public class ServiceMapperImpl implements ServiceMapper {

    @Override
    public Services toEntity(ServiceToAdd serviceToAdd, CurrencyRepository currencyRepository) {
        if ( serviceToAdd == null ) {
            return null;
        }

        Services.ServicesBuilder services = Services.builder();

        services.currency( mapCurrency( serviceToAddCurrencySymbol( serviceToAdd ), currencyRepository ) );
        services.description( serviceToAdd.description() );
        services.amount( serviceToAdd.amount() );

        services.lastPayment( serviceToAdd.isPaid() != null && serviceToAdd.isPaid() ? java.time.LocalDate.now() : null );

        return services.build();
    }

    @Override
    public ServiceRecord toRecord(Services services) {
        if ( services == null ) {
            return null;
        }

        CurrencyRecord currency = null;
        Long id = null;
        String description = null;
        BigDecimal amount = null;
        LocalDate lastPayment = null;

        currency = currencyToCurrencyRecord( services.getCurrency() );
        id = services.getId();
        description = services.getDescription();
        amount = services.getAmount();
        lastPayment = services.getLastPayment();

        Boolean isPaid = services.getIsPaid();

        ServiceRecord serviceRecord = new ServiceRecord( id, description, amount, currency, lastPayment, isPaid );

        return serviceRecord;
    }

    private String serviceToAddCurrencySymbol(ServiceToAdd serviceToAdd) {
        CurrencyRecord currency = serviceToAdd.currency();
        if ( currency == null ) {
            return null;
        }
        return currency.symbol();
    }

    protected CurrencyRecord currencyToCurrencyRecord(Currency currency) {
        if ( currency == null ) {
            return null;
        }

        String symbol = null;

        symbol = currency.getSymbol();

        CurrencyRecord currencyRecord = new CurrencyRecord( symbol );

        return currencyRecord;
    }
}
