package api.m2.movements.records.accounts;

import api.m2.movements.records.users.UserBaseRecord;

public record AccountMemberRecord(Long id,
                                  UserBaseRecord user,
                                  String role) {
}
