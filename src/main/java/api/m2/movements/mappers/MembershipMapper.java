package api.m2.movements.mappers;

import api.m2.movements.entities.Account;
import api.m2.movements.entities.AccountMember;
import api.m2.movements.records.accounts.GroupRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface MembershipMapper {

    GroupRecord toRecord(AccountMember accountMember);
    List<GroupRecord> toRecord(List<Account> account);
}
