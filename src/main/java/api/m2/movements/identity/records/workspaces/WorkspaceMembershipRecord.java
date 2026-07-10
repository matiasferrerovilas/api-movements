package api.m2.movements.identity.records.workspaces;

import api.m2.movements.identity.records.users.UserBaseRecord;

import java.util.List;

public record WorkspaceMembershipRecord(Long id,
                                        String name,
                                        UserBaseRecord owner,
                                        List<WorkspaceMemberRecord> members) {
}
