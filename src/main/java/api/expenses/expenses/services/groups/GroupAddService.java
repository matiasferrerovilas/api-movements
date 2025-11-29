package api.expenses.expenses.services.groups;

import api.expenses.expenses.aspect.interfaces.PublishMovement;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.records.groups.AddGroupRecord;
import api.expenses.expenses.records.groups.GroupsWIthUser;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.repositories.GroupRepository;
import api.expenses.expenses.repositories.UserRepository;
import api.expenses.expenses.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupAddService {
    private final GroupGetService groupGetService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final GroupRepository groupRepository;

    @PublishMovement(eventType = EventType.GROUP_ADDED, routingKey = "/topic/groups/new")
    public List<GroupsWIthUser> saveGroup(AddGroupRecord addGroupRecord) {
        var user = userService.getAuthenticatedUser();

        var group = groupGetService.getGroupByDescription(addGroupRecord.description());
        user.getUserGroups().add(group);
        userRepository.save(user);
        return groupGetService.getMyGroupsWithCount();

    }

    @PublishMovement(eventType = EventType.GROUP_DELETED, routingKey = "/topic/groups/update")
    public List<GroupsWIthUser> exitGroup(Long groupId) throws AccessDeniedException {
        var user = userService.getAuthenticatedUserRecord();

        if (!(groupRepository.userBelongsToGroup(user.id(),groupId) > 0)) {
            throw new AccessDeniedException("User does not belong to this group");
        }

        groupRepository.deleteUserFromGroup(user.id(), groupId);
        return groupGetService.getMyGroupsWithCount();
    }
}
