package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-23T17:56:34-0300",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.2.1.jar, environment: Java 25 (Amazon.com Inc.)"
)
@Component
public class CurrencyMapperImpl implements CurrencyMapper {

    @Override
    public List<CurrencyRecord> toRecordList(List<Currency> all) {
        if ( all == null ) {
            return null;
        }

        List<CurrencyRecord> list = new ArrayList<CurrencyRecord>( all.size() );
        for ( Currency currency : all ) {
            list.add( currencyToCurrencyRecord( currency ) );
        }

        return list;
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
