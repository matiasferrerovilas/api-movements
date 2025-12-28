package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.AccountInvitation;
import api.expenses.expenses.records.accounts.AccountInvitationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface AccountInvitationMapper {

    @Mapping(target = "nameAccount", source = "account.name")
    AccountInvitationRecord toRecord(AccountInvitation account);

    List<AccountInvitationRecord> toRecord(List<AccountInvitation> account);
}
