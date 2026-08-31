package api.m2.movements.services.goals;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.entities.Goal;
import api.m2.movements.enums.MembershipDomain;
import api.m2.movements.enums.NotificationSeverity;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.mappers.GoalMapper;
import api.m2.movements.records.goals.GoalContribution;
import api.m2.movements.records.goals.GoalRecord;
import api.m2.movements.records.goals.GoalToAdd;
import api.m2.movements.records.goals.GoalToUpdate;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.repositories.GoalRepository;
import api.m2.movements.services.currencies.WorkspaceCurrencyService;
import api.m2.movements.services.notifications.NotificationService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoalAddService {

    private final GoalRepository goalRepository;
    private final GoalMapper goalMapper;
    private final CurrencyRepository currencyRepository;
    private final WorkspaceCurrencyService workspaceCurrencyService;
    private final WorkspaceQueryService workspaceQueryService;
    private final NotificationService notificationService;

    @Transactional
    public GoalRecord save(@Valid GoalToAdd dto) {
        workspaceQueryService.verifyCanWrite(dto.workspaceId());

        var goal = goalMapper.toEntity(dto, currencyRepository);
        goal.setWorkspaceId(dto.workspaceId());
        goal.setCurrentAmount(BigDecimal.ZERO);
        workspaceCurrencyService.ensureCurrencyInWorkspace(dto.workspaceId(), goal.getCurrency());

        var saved = goalRepository.save(goal);
        log.debug("Meta de ahorro creada: workspaceId={}, name={}", dto.workspaceId(), dto.name());
        return goalMapper.toRecord(saved);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.GOAL, idParamIndex = 1)
    public GoalRecord update(@Valid GoalToUpdate dto, Long id) {
        var goal = this.findOrThrow(id);

        if (dto.name() != null) {
            goal.setName(dto.name());
        }
        if (dto.targetAmount() != null) {
            goal.setTargetAmount(dto.targetAmount());
        }
        // A diferencia de name/targetAmount, targetDate SIEMPRE se pisa con lo que venga —
        // null es la forma documentada de borrarla (sin fecha objetivo), no "no tocar el campo".
        goal.setTargetDate(dto.targetDate());

        var saved = goalRepository.save(goal);
        return goalMapper.toRecord(saved);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.GOAL, idParamIndex = 1)
    public GoalRecord contribute(@Valid GoalContribution dto, Long id) {
        var goal = this.findOrThrow(id);
        var amountBefore = goal.getCurrentAmount();
        var amountAfter = amountBefore.add(dto.amount());
        goal.setCurrentAmount(amountAfter);

        var saved = goalRepository.save(goal);
        log.debug("Contribución registrada en meta {}: +{}", id, dto.amount());

        if (amountBefore.compareTo(goal.getTargetAmount()) < 0 && amountAfter.compareTo(goal.getTargetAmount()) >= 0) {
            notificationService.publish(goal.getWorkspaceId(), "¡Meta de ahorro alcanzada!",
                    goal.getName() + " — $" + amountAfter + "/$" + goal.getTargetAmount(),
                    NotificationSeverity.SUCCESS);
        }

        return goalMapper.toRecord(saved);
    }

    @Transactional
    @RequiresMembership(domain = MembershipDomain.GOAL)
    public void delete(Long id) {
        if (!goalRepository.existsById(id)) {
            throw new EntityNotFoundException("Meta de ahorro no encontrada: " + id);
        }
        goalRepository.deleteById(id);
    }

    private Goal findOrThrow(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Meta de ahorro no encontrada: " + id));
    }
}
