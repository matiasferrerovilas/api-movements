package api.m2.movements.investment.services.valuation;

import api.m2.movements.investment.enums.InvestmentCategory;
import api.m2.movements.investment.records.InvestmentRecord;
import api.m2.movements.investment.records.InvestmentValuationRecord;
import api.m2.movements.investment.services.InvestmentQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvestmentValuationService {

    private final InvestmentQueryService investmentQueryService;
    private final YahooFinanceClient yahooFinanceClient;
    private final PlazaFijoValueCalculator plazaFijoCalculator;

    public List<InvestmentValuationRecord> getValuations() {
        return investmentQueryService.getByWorkspace().stream()
                .map(this::computeValuation)
                .toList();
    }

    private InvestmentValuationRecord computeValuation(InvestmentRecord investment) {
        var category = investment.investmentType().category();
        return switch (category) {
            case STOCK_ETF -> buildStockValuation(investment);
            case PLAZO_FIJO -> buildPlazaFijoValuation(investment);
            case FCI -> buildFciValuation(investment);
        };
    }

    private InvestmentValuationRecord buildStockValuation(InvestmentRecord investment) {
        BigDecimal currentPrice = null;
        if (investment.symbol() != null) {
            currentPrice = yahooFinanceClient.getPrice(investment.symbol()).orElse(null);
        }
        return new InvestmentValuationRecord(
                investment.id(),
                investment.description(),
                investment.amount(),
                null,
                currentPrice,
                investment.symbol(),
                InvestmentCategory.STOCK_ETF,
                investment.currency().symbol());
    }

    private InvestmentValuationRecord buildPlazaFijoValuation(InvestmentRecord investment) {
        BigDecimal currentValue = null;
        if (investment.tna() != null && investment.startDate() != null) {
            currentValue = plazaFijoCalculator.calculate(
                    investment.amount(), investment.tna(), investment.startDate());
        }
        return new InvestmentValuationRecord(
                investment.id(),
                investment.description(),
                investment.amount(),
                currentValue,
                null,
                null,
                InvestmentCategory.PLAZO_FIJO,
                investment.currency().symbol());
    }

    private InvestmentValuationRecord buildFciValuation(InvestmentRecord investment) {
        return new InvestmentValuationRecord(
                investment.id(),
                investment.description(),
                investment.amount(),
                investment.amount(),
                null,
                null,
                InvestmentCategory.FCI,
                investment.currency().symbol());
    }
}
