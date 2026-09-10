package api.m2.movements.services.balance;

import api.m2.movements.entities.MonthlySummarySeen;
import api.m2.movements.records.balance.PendingMonthlySummary;
import api.m2.movements.repositories.MonthlySummarySeenRepository;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Resuelve y marca el "cierre de mes": el recap del mes recién cerrado que el frontend muestra
 * como una compuerta a pantalla completa la primera vez que abrís la app en un mes nuevo.
 *
 * <p>El estado "visto" se computa en cada {@code GET /v1/users/me} (no se guarda un booleano que
 * habría que resetear con un cron cada mes): hay pendiente si no existe una fila
 * {@link MonthlySummarySeen} del usuario para el mes anterior. Se descarta si el workspace todavía
 * no tiene ningún movimiento — no tiene sentido interrumpir a una cuenta recién creada.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MonthCloseService {

    private final MonthlySummarySeenRepository seenRepository;
    private final MovementRepository movementRepository;
    private final WorkspaceQueryService workspaceQueryService;
    private final Clock clock;

    public Optional<PendingMonthlySummary> pendingFor(Long workspaceId, Long userId) {
        YearMonth previous = YearMonth.now(clock).minusMonths(1);
        int year = previous.getYear();
        int month = previous.getMonthValue();

        if (seenRepository.existsByWorkspaceIdAndUserIdAndYearAndMonth(workspaceId, userId, year, month)) {
            return Optional.empty();
        }
        if (!movementRepository.existsByWorkspaceId(workspaceId)) {
            return Optional.empty();
        }
        return Optional.of(new PendingMonthlySummary(year, month));
    }

    @Transactional
    public void markSeen(Long workspaceId, Long userId, int year, int month) {
        workspaceQueryService.verifyUserIsMemberOfWorkspace(workspaceId, userId);

        if (seenRepository.existsByWorkspaceIdAndUserIdAndYearAndMonth(workspaceId, userId, year, month)) {
            return;
        }
        seenRepository.save(MonthlySummarySeen.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .year(year)
                .month(month)
                .build());
        log.info("Cierre de mes {}/{} marcado como visto — workspaceId={} userId={}", month, year, workspaceId, userId);
    }
}
