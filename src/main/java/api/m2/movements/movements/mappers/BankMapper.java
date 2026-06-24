package api.m2.movements.movements.mappers;

import api.m2.movements.movements.entities.commons.Bank;
import api.m2.movements.movements.records.banks.BankRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BankMapper {
    BankRecord toRecord(Bank bank);
    List<BankRecord> toRecordList(List<Bank> banks);
}

