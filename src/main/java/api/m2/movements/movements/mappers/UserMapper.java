package api.m2.movements.movements.mappers;

import api.m2.movements.movements.entities.integrity.User;
import api.m2.movements.movements.records.users.UserBaseRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserBaseRecord toRecord(User userGroups);
    List<UserBaseRecord> toRecord(List<User> userGroups);
}
