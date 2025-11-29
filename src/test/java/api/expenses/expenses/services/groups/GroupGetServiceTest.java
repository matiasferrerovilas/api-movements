package api.expenses.expenses.services.groups;

import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.mappers.GroupMapperImpl;
import api.expenses.expenses.records.groups.GroupsWIthUser;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.repositories.GroupRepository;
import api.expenses.expenses.services.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupGetServiceTest {
    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserService userService;

    @Spy
    private GroupMapperImpl groupMapper;

    @InjectMocks
    private GroupGetService groupGetService;

    @Test
    @DisplayName("Obtiene correctamente los grupos del usuario")
    void getMyGroupsSuccess() {
        var userRecord = new UserRecord("test@test.com", List.of(), 1L);

        var entityGroups = List.of(
                UserGroups.builder().id(10L).description("TEST").build()
        );

        var mappedRecords = List.of(
                new UserGroupsRecord("TEST", 10L)
        );

        when(userService.getAuthenticatedUserRecord()).thenReturn(userRecord);
        when(groupRepository.findGroupsOfUser(1L)).thenReturn(entityGroups);
        when(groupMapper.toRecord(entityGroups)).thenReturn(mappedRecords);

        var result = groupGetService.getMyGroups();

        assertEquals(1, result.size());
        assertEquals("TEST", result.getFirst().description());
        verify(groupRepository).findGroupsOfUser(1L);
    }
    @Test
    @DisplayName("Obtiene los grupos del usuario junto con la cantidad de miembros")
    void getMyGroupsWithCountSuccess() {
        var userRecord = new UserRecord("test@test.com", List.of(), 1L);

        var resultExpected = List.of(
                new GroupsWIthUser(10L,"TEST",  3L)
        );

        when(userService.getAuthenticatedUserRecord()).thenReturn(userRecord);
        when(groupRepository.findGroupsByUserIdWithMemberCount(1L)).thenReturn(resultExpected);

        var result = groupGetService.getMyGroupsWithCount();

        assertEquals(1, result.size());
        assertEquals(3, result.getFirst().memberCount());
        verify(groupRepository).findGroupsByUserIdWithMemberCount(1L);
    }
    @Test
    @DisplayName("Obtiene un grupo existente por descripción")
    void getGroupByDescriptionExists() {
        String description = "test";
        String normalized = StringUtils.capitalize(description);

        var existingGroup = UserGroups.builder()
                .id(100L)
                .description(normalized)
                .build();

        when(groupRepository.findByDescription(normalized))
                .thenReturn(Optional.of(existingGroup));

        var result = groupGetService.getGroupByDescription(description);

        assertNotNull(result);
        assertEquals(existingGroup, result);
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crea un nuevo grupo cuando no existe por descripción")
    void getGroupByDescriptionCreateNew() {
        String desc = "comida";
        String normalized = StringUtils.capitalize(desc); // "Comida"

        var newGroup = UserGroups.builder()
                .id(200L)
                .description(normalized)
                .build();

        when(groupRepository.findByDescription(normalized))
                .thenReturn(Optional.empty());
        when(groupRepository.save(any(UserGroups.class)))
                .thenReturn(newGroup);

        var result = groupGetService.getGroupByDescription(desc);

        assertNotNull(result);
        assertEquals(normalized, result.getDescription());

        // Verificamos búsqueda con normalizado
        verify(groupRepository).findByDescription(normalized);

        // Verificamos guardado de nuevo grupo
        verify(groupRepository).save(any(UserGroups.class));
    }

}