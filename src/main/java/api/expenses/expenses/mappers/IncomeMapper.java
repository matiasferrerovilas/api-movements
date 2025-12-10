package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.Income;
import api.expenses.expenses.records.income.IncomeRecord;
import api.expenses.expenses.records.income.IncomeToAdd;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CurrencyMapper.class, UserMapper.class, GroupMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IncomeMapper {
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "userGroups", ignore = true)
    Income toEntity(IncomeToAdd incomeToAdd);

    @Mapping(source = "userGroups", target = "groups")
    IncomeRecord toRecord(Income income);
    List<IncomeRecord> toRecord(List<Income> income);

}
