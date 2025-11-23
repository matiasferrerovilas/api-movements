package api.expenses.expenses.services.groups;

import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.mappers.GroupMapper;
import api.expenses.expenses.records.groups.GroupsWIthUser;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.repositories.GroupRepository;
import api.expenses.expenses.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupGetService {
    private final GroupRepository groupRepository;
    private final UserService userService;
    private final GroupMapper groupMapper;

    public List<UserGroupsRecord> getMyGroups() {
        var user = userService.getAuthenticatedUserRecord();

        return groupMapper.toRecord(groupRepository.findGroupsOfUser(user.id()));
    }
    public List<GroupsWIthUser> getMyGroupsWithCount() {
        var user = userService.getAuthenticatedUserRecord();
        return groupRepository.findGroupsByUserIdWithMemberCount(user.id());
    }

    public UserGroups getGroupByDescription(String description) {
        return groupRepository.findByDescription(description)
                .orElseGet(() ->
                        groupRepository.save(UserGroups.builder()
                                .description(StringUtils.capitalize(description))
                                .build())
                );
    }
}
