package api.expenses.expenses.services.groups;

import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.enums.GroupsEnum;
import api.expenses.expenses.repositories.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultGroupServiceTest {

    @InjectMocks
    private DefaultGroupService defaultGroupService;

    @Mock
    private GroupRepository groupRepository;

    @Test
    @DisplayName("Debe devolver el grupo DEFAULT si existe en la BD")
    void getDefaultGroupExists() {
        UserGroups defaultGroup = UserGroups.builder()
                .description(GroupsEnum.DEFAULT.name())
                .build();

        when(groupRepository.findByDescription(GroupsEnum.DEFAULT.name()))
                .thenReturn(Optional.of(defaultGroup));

        UserGroups result = defaultGroupService.getDefaultGroup();

        assertNotNull(result);
        assertEquals(GroupsEnum.DEFAULT.name(), result.getDescription());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el grupo DEFAULT no existe")
    void getDefaultGroupNotFound() {
        when(groupRepository.findByDescription(GroupsEnum.DEFAULT.name()))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> defaultGroupService.getDefaultGroup()
        );

        assertEquals("No existe el grupo DEFAULT", ex.getMessage());
    }
}