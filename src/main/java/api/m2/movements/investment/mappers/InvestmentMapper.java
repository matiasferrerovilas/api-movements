package api.m2.movements.investment.mappers;

import api.m2.movements.investment.entities.Investment;
import api.m2.movements.investment.records.InvestmentRecord;
import api.m2.movements.investment.records.InvestmentToUpdate;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {InvestmentTypeMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvestmentMapper {

    @Mapping(target = "workspaceName", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "currency.symbol", source = "currency.symbol")
    @Mapping(target = "currency.id", source = "currency.id")
    InvestmentRecord toRecord(Investment investment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "investmentType", ignore = true)
    @Mapping(target = "workspaceId", ignore = true)
    void updateInvestment(InvestmentToUpdate dto, @MappingTarget Investment investment);
}
