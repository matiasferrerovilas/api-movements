package api.m2.movements.identity.mappers;

import api.m2.movements.identity.entities.Workspace;
import api.m2.movements.identity.records.workspaces.WorkspaceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface WorkspaceMapper {

    WorkspaceRecord toRecord(Workspace workspace);
    List<WorkspaceRecord> toRecord(List<Workspace> workspaces);
}
