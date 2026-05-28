package api.m2.movements.mappers;

import api.m2.movements.entities.investments.Investment;
import api.m2.movements.records.investments.InvestmentRecord;
import api.m2.movements.records.investments.InvestmentToUpdate;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {InvestmentTypeMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvestmentMapper {

    @Mapping(target = "workspaceId", expression = "java(investment.getWorkspace().getId())")
    @Mapping(target = "workspaceName", expression = "java(investment.getWorkspace().getName())")
    @Mapping(target = "owner", source = "owner.givenName")
    @Mapping(target = "currency.symbol", source = "currency.symbol")
    @Mapping(target = "currency.id", source = "currency.id")
    InvestmentRecord toRecord(Investment investment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "investmentType", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    void updateInvestment(InvestmentToUpdate dto, @MappingTarget Investment investment);
}
