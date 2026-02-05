package api.m2.movements.services.movements;

import api.m2.movements.mappers.MovementMapper;
import api.m2.movements.records.LastIngresoRecord;
import api.m2.movements.records.accounts.AccountRecord;
import api.m2.movements.records.movements.MovementRecord;
import api.m2.movements.records.movements.MovementSearchFilterRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.accounts.AccountQueryService;
import api.m2.movements.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementGetService {
    private final UserService userService;
    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final AccountQueryService accountQueryService;

    @Transactional
    public Page<@NonNull MovementRecord> getExpensesBy(MovementSearchFilterRecord filter, Pageable page) {
        var accounts = accountQueryService.findAllAccountsOfLogInUser()
                .stream()
                .map(AccountRecord::id)
                .toList();
        return movementRepository.getExpenseBy(accounts, filter, page)
                .map(movementMapper::toRecord);
    }

    public LastIngresoRecord getLastIngreso() {
        var user = userService.getAuthenticatedUserRecord();
        var ingreso = movementRepository.getLastIngreso(user)
                .orElseThrow(() -> new EntityNotFoundException("No se encontro ningun ingreso"));

        return movementMapper.toLastIngreso(ingreso);
    }
}
