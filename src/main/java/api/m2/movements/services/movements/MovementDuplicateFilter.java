package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.Movement;
import api.m2.movements.enums.MovementType;
import api.m2.movements.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saca de un import los movimientos que ya existen en el workspace, para que subir dos veces el
 * mismo extracto (o dos extractos que se pisan en fechas) no duplique nada.
 *
 * <p>Un movimiento se considera el mismo si coincide en fecha, importe, tipo, banco y moneda. La
 * descripción queda afuera a propósito: así no se duplica un movimiento importado al que después
 * se le cambió la descripción, ni uno que ya se había cargado a mano.
 *
 * <p>Se compara contando ocurrencias: si el extracto trae dos cafés iguales el mismo día y en la
 * base hay uno, se guarda solo el segundo.
 */
@Component
@RequiredArgsConstructor
public class MovementDuplicateFilter {

    private final MovementRepository movementRepository;

    public List<Movement> filterNew(List<Movement> candidates, Long workspaceId) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        var from = candidates.stream().map(Movement::getDate).min(Comparator.naturalOrder()).orElseThrow();
        var to = candidates.stream().map(Movement::getDate).max(Comparator.naturalOrder()).orElseThrow();

        Map<DuplicateKey, Integer> existing = new HashMap<>();
        movementRepository.findByWorkspaceIdAndDateBetween(workspaceId, from, to)
                .forEach(movement -> existing.merge(this.keyOf(movement), 1, Integer::sum));

        var result = new ArrayList<Movement>();
        for (Movement candidate : candidates) {
            var key = this.keyOf(candidate);
            int remaining = existing.getOrDefault(key, 0);
            if (remaining > 0) {
                existing.put(key, remaining - 1);
            } else {
                result.add(candidate);
            }
        }
        return result;
    }

    private DuplicateKey keyOf(Movement movement) {
        return new DuplicateKey(
                movement.getDate(),
                movement.getAmount().stripTrailingZeros(),
                movement.getType(),
                movement.getBank() == null ? null : movement.getBank().getId(),
                movement.getCurrency() == null ? null : movement.getCurrency().getId());
    }

    private record DuplicateKey(LocalDate date, BigDecimal amount, MovementType type, Long bankId, Long currencyId) {
    }
}
