package api.m2.movements.records.accounts;

import api.m2.movements.records.groups.UserRecord;

import java.util.List;

public record AccountRecord(Long id,
                            String name,
                            UserRecord owner,
                            List<AccountMemberRecord> members) {
}
