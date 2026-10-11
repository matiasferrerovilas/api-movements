package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.helpers.GeneratedLedger;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import api.m2.movements.records.movements.MovementItemDto;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementToAdd;
import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

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
        var context = assembler.contextWithoutIdentity(movement.getWorkspaceId());
        return assembler.assemble(movement, context, items);
    }

    /**
     * Genera la próxima cuota de cada compra en cuotas (CREDITO) que quedó pendiente el mes
     * pasado — clona el movimiento anterior tal cual (monto, categoría, moneda, banco,
     * lastCreditPayment) y solo cambia cuotaActual (+1) y date (mes actual). No necesita ningún
     * id que agrupe las cuotas de una misma compra: alcanza con mirar el mes anterior, porque
     * cada fila ya lleva su propio estado (cuotaActual) — la cuota que generamos hoy es, por
     * definición, la fila del mes anterior con cuotaActual todavía distinto de cuotasTotales.
     * Es idempotente: antes de generar cada cuota descuenta las que ya existen en el mes
     * siguiente (las que generó una corrida anterior o cargó el usuario a mano), así que volver
     * a correrlo no duplica. Devuelve cuántas generó en esta corrida.
     * Llamado por {@link CreditInstallmentJob}.
     *
     * shortcut: una cuota se considera ya generada si coincide workspace, owner, descripción,
     * monto, moneda y número de cuota; si el usuario edita esos campos de la cuota generada y se
     * vuelve a correr el job, se genera otra. Alcanza porque el job corre una vez por mes.
     */
    @Transactional
    public int generateNextCreditInstallments(YearMonth previousMonth) {
        var pending = movementRepository.findCreditoMovementsWithPendingInstallments(
                previousMonth.getYear(), previousMonth.getMonthValue());
        var alreadyGenerated = this.alreadyGeneratedInstallments(previousMonth.plusMonths(1));

        int generated = 0;
        for (var previous : pending) {
            if (alreadyGenerated.consume(this.installmentKey(previous, previous.getCuotaActual() + 1))) {
                continue;
            }
            this.saveMovement(this.buildNextInstallment(previous), previous.getWorkspaceId(), previous.getOwnerId());
            generated++;
        }

        log.info("Cuotas de crédito (mes anterior: {}): {} generadas, {} ya existían",
                previousMonth, generated, pending.size() - generated);
        return generated;
    }

    private GeneratedLedger<InstallmentKey> alreadyGeneratedInstallments(YearMonth month) {
        var existing = movementRepository.findCreditoInstallmentsBetween(
                month.atDay(1), month.plusMonths(1).atDay(1));
        return new GeneratedLedger<>(existing.stream()
                .map(movement -> this.installmentKey(movement, movement.getCuotaActual()))
                .toList());
    }

    private InstallmentKey installmentKey(Movement movement, Integer cuotaActual) {
        return new InstallmentKey(movement.getWorkspaceId(), movement.getOwnerId(), movement.getDescription(),
                movement.getAmount().stripTrailingZeros(), movement.getCurrency().getSymbol(),
                cuotaActual, movement.getCuotasTotales());
    }

    private record InstallmentKey(Long workspaceId, Long ownerId, String description, BigDecimal amount,
                                  String currency, Integer cuotaActual, Integer cuotasTotales) {
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
