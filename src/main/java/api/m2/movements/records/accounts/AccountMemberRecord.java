package api.m2.movements.records.accounts;

import api.m2.movements.records.groups.UserRecord;

public record AccountMemberRecord(Long id,
                                  UserRecord user,
                                  String role) {
}
