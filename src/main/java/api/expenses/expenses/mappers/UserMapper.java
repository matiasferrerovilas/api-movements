package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.User;
import api.expenses.expenses.records.groups.UserRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  uses = {UserMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserRecord toRecord(User userGroups);
    List<UserRecord> toRecord(List<User> userGroups);
}
