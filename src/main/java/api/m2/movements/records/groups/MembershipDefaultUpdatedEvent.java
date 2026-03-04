package api.m2.movements.records.groups;

import api.m2.movements.records.accounts.GroupRecord;
import lombok.Builder;

@Builder
public record MembershipDefaultUpdatedEvent(String logInuser, GroupRecord groupUpdated) {
}
