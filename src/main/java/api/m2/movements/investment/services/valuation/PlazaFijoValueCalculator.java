package api.m2.movements.investment.services.valuation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class PlazaFijoValueCalculator {

    private static final int RATE_SCALE = 10;

    public BigDecimal calculate(BigDecimal amount, BigDecimal tna, LocalDate startDate) {
        long days = ChronoUnit.DAYS.between(startDate, LocalDate.now(ZoneOffset.UTC));
        if (days <= 0) {
            return amount;
        }
        // currentValue = amount * (1 + tna / 365 * days)
        BigDecimal rate = tna.divide(new BigDecimal("365"), RATE_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(days));
        return amount.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
    }
}
