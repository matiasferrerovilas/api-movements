package api.m2.movements.movements.mappers;

import api.m2.movements.movements.entities.commons.Currency;
import api.m2.movements.movements.records.currencies.CurrencyRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurrencyMapper {
    List<CurrencyRecord> toRecordList(List<Currency> all);
    CurrencyRecord toRecord(Currency currency);
}
