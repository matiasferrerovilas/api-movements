package api.expenses.expenses.records.accounts;

import api.expenses.expenses.records.groups.UserRecord;

import java.util.List;

public record AccountRecord(Long id,
                            String name,
                            UserRecord owner,
                            List<AccountMemberRecord> members) {
}
