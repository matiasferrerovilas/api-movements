package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurrencyMapper {
    List<CurrencyRecord> toRecordList(List<Currency> all);
    CurrencyRecord toRecord(Currency currency);
}
