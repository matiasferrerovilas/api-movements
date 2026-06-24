package api.m2.movements.movements.records.workspaces;

import api.m2.movements.movements.records.users.UserBaseRecord;

import java.util.List;

public record WorkspaceRecord(Long id,
                               String name,
                               UserBaseRecord owner,
                               List<WorkspaceMemberRecord> members) {
}
