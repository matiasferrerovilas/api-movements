package api.m2.movements.mappers;

import api.m2.movements.entities.AccountInvitation;
import api.m2.movements.records.groups.GroupInvitationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupInvitationMapper {
    List<GroupInvitationRecord> toRecord(List<AccountInvitation> accountInvitations);
}
