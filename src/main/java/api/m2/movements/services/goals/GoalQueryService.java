package api.m2.movements.services.goals;

import api.m2.movements.mappers.GoalMapper;
import api.m2.movements.records.goals.GoalRecord;
import api.m2.movements.repositories.GoalRepository;
import api.m2.movements.services.user.UserService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoalQueryService {

    private final GoalRepository goalRepository;
    private final GoalMapper goalMapper;
    private final WorkspaceQueryService workspaceQueryService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<GoalRecord> getByWorkspace(Long workspaceId) {
        Long userId = userService.getMe().id();
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);

        return goalRepository.findByWorkspaceId(workspaceId).stream()
                .map(goalMapper::toRecord)
                .toList();
    }
}
