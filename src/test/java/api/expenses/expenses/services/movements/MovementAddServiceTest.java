package api.expenses.expenses.services.movements;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.mappers.MovementMapperImpl;
import api.expenses.expenses.records.movements.ExpenseToUpdate;
import api.expenses.expenses.records.movements.MovementRecord;
import api.expenses.expenses.records.movements.MovementToAdd;
import api.expenses.expenses.repositories.MovementRepository;
import api.expenses.expenses.services.publishing.websockets.MovementPublishServiceWebSocket;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementAddServiceTest {
    @Mock
    private MovementRepository movementRepository;
    @Mock
    private MovementMapperImpl movementMapper;
    @Mock
    private MovementFactory movementFactory;
    @Mock
    private MovementPublishServiceWebSocket movementPublishService;

    @InjectMocks
    private MovementAddService movementAddService;

    @Test
    @DisplayName("Guarda un movimiento correctamente y publica evento")
    void saveMovementSuccess() {
        var dto = mock(MovementToAdd.class);
        var entity = mock(Movement.class);
        var record = mock(MovementRecord.class);

        when(movementFactory.create(dto)).thenReturn(entity);
        when(movementRepository.save(entity)).thenReturn(entity);
        when(movementMapper.toRecord(entity)).thenReturn(record);

        var result = movementAddService.saveMovement(dto);

        assertNotNull(result);
        verify(movementPublishService).publishMovementAdded(record);
    }

    @Test
    @DisplayName("Actualiza un movimiento existente y aplica cambios correctamente")
    void updateMovementSuccess() {
        Long id = 10L;
        var dto = mock(ExpenseToUpdate.class);
        Movement movement = mock(Movement.class);

        when(movementRepository.findById(id)).thenReturn(Optional.of(movement));

        movementAddService.updateMovement(dto, id);

        verify(movementMapper).updateMovement(dto, movement);
        verify(movementFactory).applyUpdates(dto, movement);
        verify(movementRepository).save(movement);
    }

    @Test
    @DisplayName("Lanza excepción al intentar actualizar movimiento inexistente")
    void updateMovementNotFound() {
        Long id = 99L;
        ExpenseToUpdate dto = mock(ExpenseToUpdate.class);

        when(movementRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> movementAddService.updateMovement(dto, id));

        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guarda una lista de movimientos y publica evento")
    void saveExpenseAllSuccess() {
        MovementToAdd dto1 = mock(MovementToAdd.class);
        MovementToAdd dto2 = mock(MovementToAdd.class);

        Movement mov1 = mock(Movement.class);
        Movement mov2 = mock(Movement.class);

        MovementRecord rec1 = mock(MovementRecord.class);
        MovementRecord rec2 = mock(MovementRecord.class);

        List<MovementToAdd> list = List.of(dto1, dto2);
        List<Movement> savedEntities = List.of(mov1, mov2);
        List<MovementRecord> records = List.of(rec1, rec2);

        when(movementFactory.create(dto1)).thenReturn(mov1);
        when(movementFactory.create(dto2)).thenReturn(mov2);
        when(movementRepository.saveAll(savedEntities)).thenReturn(savedEntities);
        when(movementMapper.toRecord(mov1)).thenReturn(rec1);
        when(movementMapper.toRecord(mov2)).thenReturn(rec2);

        List<MovementRecord> result = movementAddService.saveExpenseAll(list);

        assertNotNull(result);
        verify(movementPublishService).publishListMovementAdded(records);
        verify(movementRepository).saveAll(savedEntities);
    }


    @Test
    @DisplayName("Elimina movimiento existente y publica evento")
    void deleteMovementSuccess() {
        Long id = 5L;

        when(movementRepository.existsById(id)).thenReturn(true);

        movementAddService.deleteMovement(id);

        verify(movementRepository).deleteById(id);
        verify(movementPublishService).publishDeleteOfMovement(id);
    }

    @Test
    @DisplayName("Error al eliminar movimiento inexistente")
    void deleteMovementNotFound() {
        Long id = 66L;

        when(movementRepository.existsById(id)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> movementAddService.deleteMovement(id));

        verify(movementRepository, never()).deleteById(any());
    }
}