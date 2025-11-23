package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupMapper {
    UserGroupsRecord toRecord(UserGroups userGroups);
    List<UserGroupsRecord> toRecord(List<UserGroups> userGroups);
}
