package api.expenses.expenses.services.groups;

import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.enums.GroupsEnum;
import api.expenses.expenses.repositories.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultGroupService {

    private final GroupRepository groupRepository;

    public UserGroups getDefaultGroup() {
        return groupRepository.findByDescription(GroupsEnum.DEFAULT.name())
                .orElseThrow(() -> new EntityNotFoundException("No existe el grupo DEFAULT"));
    }
}