package api.m2.movements.helpers;

import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.repositories.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public abstract class PdfExtractorHelper {

    protected final CurrencyRepository currencyRepository;
    protected BigDecimal parseMoney(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return null;
        }

        String cleaned = amount.trim()
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(cleaned);
    }
    protected Optional<BigDecimal> parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(parseMoney(amountStr.trim()));
        } catch (Exception _) {
            log.debug("Failed to parse amount: '{}'", amountStr);
            return Optional.empty();
        }
    }
    public abstract String getBank();
    public abstract List<ParsedExpense> parse(String pdfText);
}
