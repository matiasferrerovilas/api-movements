package api.expenses.expenses.mappers;

import api.expenses.expenses.entities.AccountInvitation;
import api.expenses.expenses.records.groups.GroupInvitationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupInvitationMapper {
    List<GroupInvitationRecord> toRecord(List<AccountInvitation> accountInvitations);
}
