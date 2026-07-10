package api.m2.movements.identity.records.workspaces;

import api.m2.movements.identity.records.users.UserBaseRecord;

public record WorkspaceMemberRecord(Long id,
                                    UserBaseRecord user,
                                    String role) {
}
