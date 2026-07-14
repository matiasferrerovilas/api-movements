package api.m2.movements.mappers;

import api.m2.movements.entities.movements.Income;
import api.m2.movements.records.income.IncomeRecord;
import api.m2.movements.records.income.IncomeToAdd;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IncomeMapper {
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "workspaceId", ignore = true)
    @Mapping(target = "bank", ignore = true)
    Income toEntity(IncomeToAdd incomeToAdd);

    @Mapping(target = "accountName", ignore = true)
    @Mapping(source = "bank.description", target = "bank")
    IncomeRecord toRecord(Income income);
    List<IncomeRecord> toRecord(List<Income> income);
}
