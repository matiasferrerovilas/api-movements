package api.m2.movements.mappers;

import api.m2.movements.entities.WorkspaceInvitation;
import api.m2.movements.records.invite.InvitationToWorkspaceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface WorkspaceInvitationMapper {

    @Mapping(target = "workspaceName", source = "workspace.name")
    @Mapping(target = "invitedBy", source = "invitedBy.email")
    @Mapping(target = "invitedUserId", source = "user.id")
    InvitationToWorkspaceRecord toRecord(WorkspaceInvitation invitation);

    List<InvitationToWorkspaceRecord> toRecord(List<WorkspaceInvitation> invitations);
}
