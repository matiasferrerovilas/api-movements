package api.m2.movements.services.movements;

import api.m2.movements.exceptions.ExchangeRateNotFoundException;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.currencies.ExchangeRateResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrateExchangeRateService {

    private final MovementRepository movementRepository;
    private final ExchangeRateResolver exchangeRateResolver;

    @Transactional
    public int migrateAll() {
        var movements = movementRepository.findAllByExchangeRateIsNull();
        int successCount = 0;
        int failCount = 0;

        for (var movement : movements) {
            try {
                var rate = exchangeRateResolver.resolveRate(
                        movement.getCurrency().getSymbol(),
                        movement.getDate()
                );
                movement.setExchangeRate(rate);
                movementRepository.save(movement);
                successCount++;
            } catch (ExchangeRateNotFoundException e) {
                log.warn("No se pudo obtener exchange rate para movimiento {}: {}",
                        movement.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("Migración de exchange_rate completada: {} exitosos, {} fallidos de {} totales",
                successCount, failCount, movements.size());
        return successCount;
    }
}
