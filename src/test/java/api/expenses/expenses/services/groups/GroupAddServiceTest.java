package api.expenses.expenses.services.groups;

import api.expenses.expenses.entities.User;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.records.groups.AddGroupRecord;
import api.expenses.expenses.records.groups.GroupsWIthUser;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.repositories.GroupRepository;
import api.expenses.expenses.repositories.UserRepository;
import api.expenses.expenses.services.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupAddServiceTest {
    @Mock
    private GroupGetService groupGetService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private GroupRepository groupRepository;
    @InjectMocks
    private GroupAddService groupAddService;

    @Test
    @DisplayName("saveGroup: el grupo es agregado correctamente al usuario")
    void saveGroupSuccess() {
        AddGroupRecord addGroupRecord = new AddGroupRecord("Deportes");

        var user = User.builder()
                .id(1L)
                .userGroups(new HashSet<>())
                .build();

        var group = UserGroups.builder()
                .id(10L)
                .description("DEPORTES")
                .build();

        List<GroupsWIthUser> expected = List.of(mock(GroupsWIthUser.class));

        when(userService.getAuthenticatedUser()).thenReturn(user);
        when(groupGetService.getGroupByDescription("Deportes")).thenReturn(group);
        when(groupGetService.getMyGroupsWithCount()).thenReturn(expected);

        List<GroupsWIthUser> result = groupAddService.saveGroup(addGroupRecord);

        assertEquals(1, user.getUserGroups().size());
        assertTrue(user.getUserGroups().contains(group));
        assertEquals(expected, result);

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("exitGroup: el usuario sale correctamente del grupo")
    void exitGroupSuccess() throws AccessDeniedException {
        Long groupId = 10L;

        var userRecord = new UserRecord(
                "test@test.com",
                List.of(new UserGroupsRecord("TEST", groupId)),
                1L
        );
        List<GroupsWIthUser> expected = List.of(mock(GroupsWIthUser.class));

        when(userService.getAuthenticatedUserRecord()).thenReturn(userRecord);
        when(groupRepository.userBelongsToGroup(userRecord.id(), groupId)).thenReturn(1);
        when(groupGetService.getMyGroupsWithCount()).thenReturn(expected);

        List<GroupsWIthUser> result = groupAddService.exitGroup(groupId);

        assertEquals(expected, result);

        verify(groupRepository).deleteUserFromGroup(userRecord.id(), groupId);
    }
    @Test
    @DisplayName("Falla al intentar salir de un grupo al que no pertenece")
    void exitGroupAccessDenied() {
        long groupId = 10L;

        var userRecord = new UserRecord("test@test.com", List.of(), 1L);

        when(userService.getAuthenticatedUserRecord()).thenReturn(userRecord);
        when(groupRepository.userBelongsToGroup(1L, groupId)).thenReturn(0);

        assertThrows(AccessDeniedException.class, () -> groupAddService.exitGroup(groupId));
    }
}