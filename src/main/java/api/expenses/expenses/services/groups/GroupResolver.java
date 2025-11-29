package api.expenses.expenses.services.groups;

import api.expenses.expenses.entities.UserGroups;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupResolver {
    private final DefaultGroupService defaultGroupService;
    private final GroupGetService groupGetService;

    public UserGroups resolve(String groupDescription) {
        if (groupDescription == null) return defaultGroupService.getDefaultGroup();

        return groupGetService.getGroupByDescription(groupDescription);
    }
}
