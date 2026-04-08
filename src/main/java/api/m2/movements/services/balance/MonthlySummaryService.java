package api.m2.movements.services.balance;

import api.m2.movements.enums.MovementType;
import api.m2.movements.records.balance.MonthlySummaryComparisonRecord;
import api.m2.movements.records.balance.MonthlySummaryRecord;
import api.m2.movements.repositories.MovementRepository;
import api.m2.movements.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class MonthlySummaryService {

    private final MovementRepository movementRepository;
    private final UserService userService;

    public MonthlySummaryRecord getSummary(Integer year, Integer month) {
        var email = userService.getAuthenticatedUser().getEmail();

        BigDecimal ingresado = this.getTotalByType(email, year, month, MovementType.INGRESO);
        BigDecimal gastado = this.getTotalByType(email, year, month, MovementType.DEBITO);
        String topCategory = movementRepository.getTopCategoryByMonth(email, year, month).orElse(null);

        YearMonth prevYearMonth = YearMonth.of(year, month).minusMonths(1);
        int prevYear = prevYearMonth.getYear();
        int prevMonth = prevYearMonth.getMonthValue();

        BigDecimal ingresadoAnterior = this.getTotalByType(email, prevYear, prevMonth, MovementType.INGRESO);
        BigDecimal gastadoAnterior = this.getTotalByType(email, prevYear, prevMonth, MovementType.DEBITO);

        return new MonthlySummaryRecord(
                year,
                month,
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

    private BigDecimal getTotalByType(String email, int year, int month, MovementType type) {
        return movementRepository.getTotalByTypeAndMonth(email, year, month, type.name());
    }
}
