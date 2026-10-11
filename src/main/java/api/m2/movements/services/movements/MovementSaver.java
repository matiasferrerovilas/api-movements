package api.m2.movements.services.movements;

import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementToAdd;
import jakarta.validation.Valid;

/**
 * Contrato de alta de un movimiento en un workspace concreto: persiste, reemplaza el desglose y
 * publica el {@link MovementRecord} resultante. Lo implementan {@link MovementAddService} (dentro de
 * un request autenticado) y {@link SystemMovementAddService} (jobs programados, sin JWT).
 */
public interface MovementSaver {

    MovementRecord saveMovement(@Valid MovementToAdd dto, Long workspaceId, Long ownerId);
}
