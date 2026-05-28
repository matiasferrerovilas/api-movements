package api.m2.movements.mappers;

import api.m2.movements.entities.investments.InvestmentType;
import api.m2.movements.records.investments.InvestmentTypeRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvestmentTypeMapper {

    @Mapping(target = "workspaceId", expression = "java(investmentType.getWorkspace().getId())")
    InvestmentTypeRecord toRecord(InvestmentType investmentType);

    List<InvestmentTypeRecord> toRecordList(List<InvestmentType> types);
}
