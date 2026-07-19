package api.m2.movements.mappers;

import api.m2.movements.entities.commons.Bank;
import api.m2.movements.records.banks.BankRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BankMapper {
    BankRecord toRecord(Bank bank);
}

