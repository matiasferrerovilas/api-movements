package api.m2.movements.mappers;

import api.m2.movements.entities.Account;
import api.m2.movements.entities.AccountMember;
import api.m2.movements.records.accounts.AccountMemberRecord;
import api.m2.movements.records.accounts.GroupRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface MembershipMapper {

    AccountMemberRecord toRecord(AccountMember accountMember);
    List<AccountMemberRecord> toRecord(List<AccountMember> account);
}
