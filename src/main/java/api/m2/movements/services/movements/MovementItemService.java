package api.m2.movements.services.movements;

import api.m2.movements.entities.movements.MovementItem;
import api.m2.movements.enums.MovementItemUnit;
import api.m2.movements.records.movements.MovementItemDto;
import api.m2.movements.repositories.MovementItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El desglose de un movimiento viaja siempre embebido en su {@code MovementRecord} (ver
 * MovementGetService/MovementAddService) — no hay endpoint público por id, así que este servicio
 * no necesita repetir el chequeo de workspace/membership: quien llama ya resolvió esa scoping al
 * traer el/los {@code Movement} correspondientes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovementItemService {
    private final MovementItemRepository movementItemRepository;

    @Transactional(readOnly = true)
    public List<MovementItemDto> getItems(Long movementId) {
        return movementItemRepository.findByMovementIdOrderByIdAsc(movementId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Batch usado por el listado paginado — evita una consulta por fila. */
    @Transactional(readOnly = true)
    public Map<Long, List<MovementItemDto>> getItemsByMovementIds(List<Long> movementIds) {
        if (movementIds.isEmpty()) {
            return Map.of();
        }
        return movementItemRepository.findByMovementIdInOrderByIdAsc(movementIds).stream()
                .collect(Collectors.groupingBy(
                        MovementItem::getMovementId,
                        Collectors.mapping(this::toDto, Collectors.toList())));
    }

    /**
     * Reemplaza por completo el desglose de un movimiento. Llamado desde MovementAddService al
     * crear (siempre, si dto.items() no es null) o actualizar (solo si dto.items() no es null —
     * null significa "no tocar", ver ExpenseToUpdate.items).
     */
    @Transactional
    public void replaceItems(Long movementId, List<MovementItemDto> items) {
        movementItemRepository.deleteByMovementId(movementId);
        if (items == null || items.isEmpty()) {
            return;
        }

        var entities = items.stream()
                .map(dto -> MovementItem.builder()
                        .movementId(movementId)
                        .quantity(dto.quantity())
                        .unit(MovementItemUnit.valueOf(dto.unit()))
                        .description(dto.description())
                        .price(dto.price())
                        .build())
                .toList();
        movementItemRepository.saveAll(entities);
    }

    @Transactional
    public void deleteItemsOf(List<Long> movementIds) {
        if (movementIds.isEmpty()) {
            return;
        }
        movementItemRepository.deleteByMovementIdIn(movementIds);
    }

    private MovementItemDto toDto(MovementItem item) {
        return new MovementItemDto(item.getId(), item.getQuantity(), item.getUnit().name(),
                item.getDescription(), item.getPrice());
    }
}
