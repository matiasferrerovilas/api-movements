package api.m2.movements.records.accounts;

import api.m2.movements.records.users.UserBaseRecord;

import java.util.List;

public record MembershipRecord(Long id,
                               String name,
                               UserBaseRecord owner,
                               List<AccountMemberRecord> members) {
}
