package api.m2.movements.mappers;

import api.m2.movements.entities.User;
import api.m2.movements.records.groups.UserRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserRecord toRecord(User userGroups);
    List<UserRecord> toRecord(List<User> userGroups);
}
