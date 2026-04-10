package api.m2.movements.services.balance;

import api.m2.movements.enums.MovementType;
import api.m2.movements.records.balance.MonthlySummaryByCurrencyRecord;
import api.m2.movements.records.balance.MonthlySummaryComparisonRecord;
import api.m2.movements.records.balance.MonthlySummaryResponse;
import api.m2.movements.records.balance.MonthlySummaryUnifiedRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlySummaryService {

    private final MovementRepository movementRepository;
    private final UserService userService;

    public MonthlySummaryResponse getSummary(Integer year, Integer month) {
        var email = userService.getAuthenticatedUser().getEmail();

        YearMonth prev = YearMonth.of(year, month).minusMonths(1);
        int prevYear = prev.getYear();
        int prevMonth = prev.getMonthValue();

        List<String> currencies = movementRepository
                .findDistinctCurrenciesByMonth(email, year, month, prevYear, prevMonth);

        List<MonthlySummaryByCurrencyRecord> porMoneda = currencies.stream()
                .map(currency -> this.buildCurrencySummary(email, year, month, prevYear, prevMonth, currency))
                .toList();

        MonthlySummaryUnifiedRecord totalUnificadoUSD = this.buildUnifiedUsd(email, year, month, prevYear, prevMonth);

        return new MonthlySummaryResponse(year, month, totalUnificadoUSD, porMoneda);
    }

    private MonthlySummaryByCurrencyRecord buildCurrencySummary(String email, int year, int month,
                                                                 int prevYear, int prevMonth, String currency) {
        BigDecimal ingresado = this.getTotalByCurrency(email, year, month, MovementType.INGRESO, currency);
        BigDecimal gastado = this.getTotalByCurrency(email, year, month, MovementType.DEBITO, currency);
        String topCategory = movementRepository.getTopCategoryByMonth(email, year, month, currency).orElse(null);

        BigDecimal ingresadoAnterior = this.getTotalByCurrency(email, prevYear, prevMonth, MovementType.INGRESO, currency);
        BigDecimal gastadoAnterior = this.getTotalByCurrency(email, prevYear, prevMonth, MovementType.DEBITO, currency);

        return new MonthlySummaryByCurrencyRecord(
                currency,
                ingresado,
                gastado,
                ingresado.subtract(gastado),
                topCategory,
                new MonthlySummaryComparisonRecord(
                        ingresadoAnterior,
                        gastadoAnterior,
                        gastado.subtract(gastadoAnterior),
                        ingresado.subtract(ingresadoAnterior)
                )
        );
    }

    private MonthlySummaryUnifiedRecord buildUnifiedUsd(String email, int year, int month,
                                                         int prevYear, int prevMonth) {
        BigDecimal ingresado = this.getTotalInUsd(email, year, month, MovementType.INGRESO);
        BigDecimal gastado = this.getTotalInUsd(email, year, month, MovementType.DEBITO);

        BigDecimal ingresadoAnterior = this.getTotalInUsd(email, prevYear, prevMonth, MovementType.INGRESO);
        BigDecimal gastadoAnterior = this.getTotalInUsd(email, prevYear, prevMonth, MovementType.DEBITO);

        return new MonthlySummaryUnifiedRecord(
                ingresado,
                gastado,
                ingresado.subtract(gastado),
                new MonthlySummaryComparisonRecord(
                        ingresadoAnterior,
                        gastadoAnterior,
                        gastado.subtract(gastadoAnterior),
                        ingresado.subtract(ingresadoAnterior)
                )
        );
    }

    private BigDecimal getTotalByCurrency(String email, int year, int month, MovementType type, String currency) {
        return movementRepository.getTotalByTypeAndMonth(email, year, month, type.name(), currency);
    }

    private BigDecimal getTotalInUsd(String email, int year, int month, MovementType type) {
        return movementRepository.getTotalInUsdByTypeAndMonth(email, year, month, type.name());
    }
}
