package api.m2.movements.records.workspaces;

import api.m2.movements.records.users.UserBaseRecord;

public record WorkspaceMemberRecord(Long id,
                                    UserBaseRecord user,
                                    String role) {
}
