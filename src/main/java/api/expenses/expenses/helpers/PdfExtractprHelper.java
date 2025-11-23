package api.expenses.expenses.helpers;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.records.pdf.ParsedExpense;
import api.expenses.expenses.repositories.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public abstract class PdfExtractprHelper {

    protected final CurrencyRepository currencyRepository;
    protected static final Pattern FOREIGN_CURRENCY_PATTERN = Pattern.compile("(CHF|USD|EUR)\\s+[\\d.,]+");
    protected static final Pattern INSTALLMENT_PATTERN = Pattern.compile("(\\d{2}/\\d{2})");


    protected DateTimeFormatter getDateFormat() {
        return DateTimeFormatter.ofPattern("dd-MM-yy");
    }
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
        } catch (Exception e) {
            log.debug("Failed to parse amount: '{}'", amountStr);
            return Optional.empty();
        }
    }
    public abstract BanksEnum getBank();
    public abstract List<ParsedExpense> parse(String pdfText);
}