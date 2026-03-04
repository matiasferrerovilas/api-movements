package api.m2.movements.mappers;

import api.m2.movements.entities.AccountInvitation;
import api.m2.movements.records.invite.InvitationToGroupRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface AccountInvitationMapper {

    @Mapping(target = "nameAccount", source = "account.name")
    @Mapping(target = "invitedBy", source = "account.owner.email")
    InvitationToGroupRecord toRecord(AccountInvitation account);

    List<InvitationToGroupRecord> toRecord(List<AccountInvitation> account);
}
