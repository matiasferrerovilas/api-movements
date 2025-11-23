package api.expenses.expenses.records.groups;

import java.util.List;

public record UserRecord(String email, List<UserGroupsRecord> userGroups, Long id) {
}
