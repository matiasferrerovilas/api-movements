package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import api.m2.movements.records.movements.MovementItemDto;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.records.workspaces.WorkspaceBaseRecord;
import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Alta de movimientos para flujos internos (jobs programados) que no corren dentro de un request
 * autenticado y por lo tanto no tienen un JWT de usuario disponible para llamar a api-identity.
 * Omite el enriquecimiento con nombre de workspace/owner en vez de fallar con
 * {@link api.m2.movements.exceptions.PermissionDeniedException}. Los flujos con usuario autenticado
 * usan {@link MovementAddService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemMovementAddService implements MovementSaver {

    private final MovementRepository movementRepository;
    private final MovementFactory movementFactory;
    private final MovementPersister movementPersister;
    private final MovementRecordAssembler assembler;

    @Override
    @Transactional
    public MovementRecord saveMovement(MovementToAdd dto, Long workspaceId, Long ownerId) {
        var movementRecord = movementPersister.persistAndPublish(
                movementFactory.create(dto, workspaceId, ownerId), dto, this::enrich);

        log.info("Movimiento de sistema guardado: id={}, type={}", movementRecord.id(), dto.type());
        return movementRecord;
    }

    private MovementRecord enrich(Movement movement, List<MovementItemDto> items) {
        var workspace = new WorkspaceBaseRecord(movement.getWorkspaceId(), null);
        return assembler.assemble(movement, workspace, Map.of(), items, assembler.iconsOf(movement.getWorkspaceId()));
    }

    /**
     * Genera la próxima cuota de cada compra en cuotas (CREDITO) que quedó pendiente el mes
     * pasado — clona el movimiento anterior tal cual (monto, categoría, moneda, banco,
     * lastCreditPayment) y solo cambia cuotaActual (+1) y date (mes actual). No necesita ningún
     * id que agrupe las cuotas de una misma compra: alcanza con mirar el mes anterior, porque
     * cada fila ya lleva su propio estado (cuotaActual) — la cuota que generamos hoy es, por
     * definición, la fila del mes anterior con cuotaActual todavía distinto de cuotasTotales.
     * Llamado por {@link CreditInstallmentJob}.
     */
    @Transactional
    public int generateNextCreditInstallments(YearMonth previousMonth) {
        var pending = movementRepository.findCreditoMovementsWithPendingInstallments(
                previousMonth.getYear(), previousMonth.getMonthValue());
        log.info("Generando {} próximas cuotas de crédito (mes anterior: {})", pending.size(), previousMonth);

        for (var previous : pending) {
            this.saveMovement(this.buildNextInstallment(previous), previous.getWorkspaceId(), previous.getOwnerId());
        }

        return pending.size();
    }

    private MovementToAdd buildNextInstallment(Movement previous) {
        var categories = previous.getCategories().stream()
                .map(c -> new CategoryUpdateRecord(null, c.getDescription()))
                .toList();

        return new MovementToAdd(
                previous.getAmount(),
                LocalDate.now(ZoneOffset.UTC),
                previous.getDescription(),
                categories,
                previous.getType().name(),
                previous.getCurrency().getSymbol(),
                previous.getCuotaActual() + 1,
                previous.getCuotasTotales(),
                previous.getBank() != null ? previous.getBank().getDescription() : null,
                previous.getLastCreditPayment(),
                null);
    }
}
