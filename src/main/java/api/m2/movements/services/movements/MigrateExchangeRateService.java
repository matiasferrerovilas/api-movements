package api.m2.movements.services.movements;

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

        movements.forEach(movement -> {
            var rate = exchangeRateResolver.resolveRate(
                    movement.getCurrency().getSymbol(),
                    movement.getDate()
            );
            movement.setExchangeRate(rate);
        });

        movementRepository.saveAll(movements);

        log.info("Migración de exchange_rate completada: {} movimientos procesados", movements.size());
        return movements.size();
    }
}
