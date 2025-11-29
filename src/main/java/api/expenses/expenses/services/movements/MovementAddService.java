package api.expenses.expenses.services.movements;

import api.expenses.expenses.aspect.interfaces.PublishMovement;
import api.expenses.expenses.enums.EventType;
import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.records.movements.ExpenseToUpdate;
import api.expenses.expenses.records.movements.MovementRecord;
import api.expenses.expenses.repositories.MovementRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovementAddService {

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementFactory movementFactory;

    @PublishMovement(eventType = EventType.MOVEMENT_ADDED, routingKey = "/topic/movimientos/new")
    public MovementRecord saveMovement(MovementToAdd dto) {
        var movement = movementFactory.create(dto);
        return movementMapper.toRecord(movementRepository.save(movement));
    }

    @Transactional
    public void updateMovement(@Valid ExpenseToUpdate dto, Long id) {
        var movement = movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));

        movementMapper.updateMovement(dto, movement);
        movementFactory.applyUpdates(dto, movement);
        movementRepository.save(movement);
    }


    @PublishMovement(eventType = EventType.MOVEMENT_ADDED, routingKey = "/topic/movimientos/history/list")
    public List<MovementRecord> saveExpenseAll(List<MovementToAdd> list) {
        return movementRepository.saveAll(
                        list.stream().map(movementFactory::create).toList()
                ).stream()
                .map(movementMapper::toRecord)
                .toList();
    }

    @Transactional
    @PublishMovement(eventType = EventType.MOVEMENT_DELETED, routingKey = "/topic/movimientos/delete")
    public Long deleteMovement(Long id) {
        movementRepository.deleteById(id);
        return id;
    }
}