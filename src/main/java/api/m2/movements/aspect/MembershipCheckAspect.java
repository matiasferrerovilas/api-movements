package api.m2.movements.aspect;

import api.m2.movements.annotations.RequiresMembership;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.repositories.BudgetRepository;
import api.m2.movements.repositories.IncomeRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.repositories.SubscriptionRepository;
import api.m2.movements.services.groups.AccountQueryService;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MembershipCheckAspect {

    private final UserService userService;
    private final AccountQueryService accountQueryService;
    private final MovementRepository movementRepository;
    private final IncomeRepository incomeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BudgetRepository budgetRepository;

    @Before("@annotation(requiresMembership)")
    public void checkMembership(JoinPoint joinPoint, RequiresMembership requiresMembership) {
        Object[] args = joinPoint.getArgs();
        Long entityId = (Long) args[requiresMembership.idParamIndex()];

        Long accountId = resolveAccountId(requiresMembership.domain(), entityId);
        Long userId = userService.getAuthenticatedUser().getId();

        log.debug("Verificando membresía: domain={}, entityId={}, accountId={}, userId={}",
                requiresMembership.domain(), entityId, accountId, userId);

        accountQueryService.verifyUserIsMemberOfAccount(accountId, userId);
    }

    private Long resolveAccountId(api.m2.movements.enums.MembershipDomain domain, Long entityId) {
        return switch (domain) {
            case MOVEMENT -> movementRepository.findById(entityId)
                    .orElseThrow(() -> new EntityNotFoundException("Movimiento no encontrado: " + entityId))
                    .getAccount().getId();
            case INCOME -> incomeRepository.findById(entityId)
                    .orElseThrow(() -> new EntityNotFoundException("Ingreso no encontrado: " + entityId))
                    .getAccount().getId();
            case SUBSCRIPTION -> subscriptionRepository.findById(entityId)
                    .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado: " + entityId))
                    .getAccount().getId();
            case BUDGET -> budgetRepository.findById(entityId)
                    .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + entityId))
                    .getAccount().getId();
        };
    }
}
