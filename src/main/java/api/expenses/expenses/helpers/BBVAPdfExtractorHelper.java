package api.expenses.expenses.helpers;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.CurrencyEnum;
import api.expenses.expenses.records.pdf.AmountInfo;
import api.expenses.expenses.records.pdf.ParsedExpense;
import api.expenses.expenses.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BBVAPdfExtractorHelper extends PdfExtractprHelper {

    private static final Pattern EXPENSE_PATTERN = Pattern.compile(
            "(\\d{2}-[A-Za-z]{3}-\\d{2})\\s+" +               // fecha
                    "(.+?)\\s+" +                                     // descripción
                    "(?:([A-Z]{3})\\s+)?" +                           // moneda opcional (USD/CLP/...)
                    "(?:(\\d{1,3}(?:[.\\d]*)?,\\d{2})\\s+)?" +        // importe en moneda extranjera (ej: 11.190,00 o 3,57)
                    "(\\d{5,})\\s+" +                                 // nro cupón
                    "(-?\\d{1,3}(?:[.\\d]*)?,\\d{2})?"                  // importe en ARS
    );

    private static final Pattern DATE_LINE_PATTERN = Pattern.compile("^\\d{2}-\\d{2}-\\d{2}.*");
    private static final Pattern INSTALLMENT_CLEANUP_PATTERN = Pattern.compile("\\s*C\\.\\d{2}/\\d{2}\\s*");
    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-yy")
            .toFormatter(Locale.forLanguageTag("es-ES"));

    public BBVAPdfExtractorHelper(CurrencyRepository currencyRepository) {
        super(currencyRepository);
    }

    @Override
    public DateTimeFormatter getDateFormat() {
        return DATE_FORMATTER;
    }

    @Override
    public BanksEnum getBank() {
        return BanksEnum.BBVA;
    }

    @Override
    public List<ParsedExpense> parse(String pdfText) {
        return pdfText.lines()
                .parallel() // Procesamiento paralelo
                .map(String::trim)
                .filter(this::isValidExpenseLine)
                .map(this::parseExpenseLine)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private boolean isValidExpenseLine(String line) {
        return !line.isEmpty();
    }

    private Optional<ParsedExpense> parseExpenseLine(String line) {
        Matcher matcher = EXPENSE_PATTERN.matcher(line);

        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            return Optional.of(this.buildParsedExpense(matcher));
        } catch (Exception e) {
            log.warn("Error parsing line: '{}' - {}", line, e.getMessage());
            return Optional.empty();
        }
    }

    private ParsedExpense buildParsedExpense(Matcher matcher) {
        var date = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
        var fullReference = matcher.group(2).trim();
        var foreignCurrency = matcher.group(3);
        var foreignAmount = matcher.group(4);
        var nroCupon = matcher.group(5);
        var arsAmount = matcher.group(6);

        var installment = this.extractInstallment(fullReference);
        var cleanedReference = this.cleanReference(fullReference, installment.isPresent());

        var amounts = this.determineAmounts(foreignCurrency, arsAmount);
        var currency = currencyRepository.findBySymbol(amounts.hasForeignCurrency() ? CurrencyEnum.USD.name() : CurrencyEnum.ARS.name())
                .orElseThrow(() -> new EntityNotFoundException("Currency not found"));

        return new ParsedExpense(
                date,
                cleanedReference,
                installment.orElse(null),
                nroCupon,
                currency,
                amounts.pesos(),
                amounts.dolares()
        );
    }

    private Optional<String> extractInstallment(String reference) {
        Matcher installmentMatcher = INSTALLMENT_PATTERN.matcher(reference);
        return installmentMatcher.find()
                ? Optional.of(installmentMatcher.group(1))
                : Optional.empty();
    }

    private String cleanReference(String reference, boolean hasInstallment) {
        if (reference == null) return null;

        String cleaned = reference.trim();

        if (hasInstallment) {
            cleaned = INSTALLMENT_CLEANUP_PATTERN.matcher(cleaned).replaceAll(" ");
        }

        return cleaned.trim();
    }

    private AmountInfo determineAmounts(String foreignCurrency, String arsAmount) {
        boolean hasForeignCurrency = foreignCurrency != null;

        BigDecimal pesos = BigDecimal.ZERO;
        BigDecimal dolares = BigDecimal.ZERO;

        if (hasForeignCurrency) {
            dolares = parseAmount(arsAmount).orElse(BigDecimal.ZERO);
        } else {
            pesos = parseAmount(arsAmount).orElse(BigDecimal.ZERO);
        }

        return new AmountInfo(pesos, dolares, hasForeignCurrency);
    }
}