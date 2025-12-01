package api.expenses.expenses.services.movements;

import api.expenses.expenses.mappers.MovementMapper;
import api.expenses.expenses.records.LastIngresoRecord;
import api.expenses.expenses.records.movements.MovementRecord;
import api.expenses.expenses.records.movements.MovementSearchFilterRecord;
import api.expenses.expenses.repositories.MovementRepository;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementGetService {
    private final UserService userService;
    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;

    @Transactional
    public Page<@NonNull MovementRecord> getExpensesBy(MovementSearchFilterRecord filter, Pageable page) {
        var user = userService.getAuthenticatedUserRecord();
        var result = movementRepository.getExpenseBy(user, filter, page);

        return result.map(movementMapper::toRecord);
    }

    public LastIngresoRecord getLastIngreso() {
        var user = userService.getAuthenticatedUserRecord();
        var ingreso = movementRepository.getLastIngreso(user)
                .orElseThrow(() -> new EntityNotFoundException("No se encontro ningun ingreso"));

        return movementMapper.toLastIngreso(ingreso);
    }
}
