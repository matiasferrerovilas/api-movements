package api.expenses.expenses.records.accounts;

import api.expenses.expenses.records.groups.UserRecord;

public record AccountMemberRecord(Long id,
                                  UserRecord user,
                                  String role) {
}
