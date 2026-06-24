package api.m2.movements.movements.mappers;

import api.m2.movements.movements.entities.integrity.WorkspaceMember;
import api.m2.movements.movements.records.workspaces.WorkspaceMemberRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",  unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface MembershipMapper {

    WorkspaceMemberRecord toRecord(WorkspaceMember workspaceMember);
    List<WorkspaceMemberRecord> toRecord(List<WorkspaceMember> members);
}
