package api.m2.movements.helpers;

import api.m2.movements.entities.commons.Currency;
import api.m2.movements.enums.MovementType;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.repositories.CurrencyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de Santander España. Un mismo banco entrega dos documentos distintos y este helper
 * detecta cuál le llegó por el encabezado:
 * <ul>
 *   <li><b>Movimientos de cuenta</b> (export "Últimos movimientos de cuenta" de la banca online):
 *   cada movimiento ocupa varias líneas (fecha, fecha valor, concepto en 1-2 líneas, importe y
 *   saldo). El signo del importe define el tipo: negativo es DEBITO, positivo es INGRESO.</li>
 *   <li><b>Extracto de tarjeta de crédito</b> ("CREDITO SANTANDER / Extracto de movimientos"):
 *   una línea por operación con fecha operación, fecha valor, concepto e importe sin símbolo €.
 *   Las líneas de la sección A (saldo anterior y pago) llevan € y quedan afuera. Un importe
 *   negativo es una devolución y se informa como REINTEGRO.</li>
 * </ul>
 * Todos los importes son en EUR.
 */
@Service
@Slf4j
public class SantanderPdfExtractorHelper extends PdfExtractorHelper {

    private static final String BANK = "SANTANDER";
    private static final String CURRENCY_SYMBOL = "EUR";

    private static final String CREDIT_MARKER = "CREDITO SANTANDER";
    private static final String ACCOUNT_MARKER = "CUENTA ONLINE SANTANDER";

    private static final DateTimeFormatter ACCOUNT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CREDIT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Pattern ACCOUNT_DATE_LINE = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");
    private static final String ACCOUNT_VALUE_DATE_PREFIX = "Fecha valor:";
    // Encabezados y pies que se repiten en cada página: si un movimiento queda partido entre dos
    // páginas no tienen que terminar pegados al concepto.
    private static final Pattern ACCOUNT_PAGE_NOISE = Pattern.compile(
            "^(TITULAR:|CUENTA ONLINE SANTANDER|Saldo:|Últimos movimientos|Fecha operación|Documento impreso|Página \\d+).*");
    private static final Pattern ACCOUNT_AMOUNT_TAIL = Pattern.compile("(-?[\\d.]+,\\d{2}) EUR\\s+-?[\\d.]+,\\d{2} EUR$");

    private static final Pattern CREDIT_LINE = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4})\\s+\\d{2}-\\d{2}-\\d{4}\\s+(.+?)\\s+(-?[\\d.]+,\\d{2})$");
    private static final int CREDIT_DATE_POSITION = 1;
    private static final int CREDIT_CONCEPT_POSITION = 2;
    private static final int CREDIT_AMOUNT_POSITION = 3;

    private static final Pattern ACCOUNT_PREFIXES = Pattern.compile(
            "^(Transaccion Contactless En|Pago Movil En|Compra Internet En|Compra Normal|Compra)\\s+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_CARD_SUFFIX = Pattern.compile(
            ",?\\s*Tarj(?:\\.|eta)\\s*:?\\s*\\*?\\s*\\d+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_PREFIXES = Pattern.compile(
            "^(TRANSACCION CONTACTLESS|COMPRA PAISES UME|COMPRA INTERNET|COMPRA)\\s+");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    public SantanderPdfExtractorHelper(CurrencyRepository currencyRepository) {
        super(currencyRepository);
    }

    @Override
    public String getBank() {
        return BANK;
    }

    @Override
    public List<ParsedExpense> parse(String pdfText) {
        if (pdfText.contains(CREDIT_MARKER)) {
            return this.parseCreditStatement(pdfText);
        }
        if (pdfText.contains(ACCOUNT_MARKER)) {
            return this.parseAccountMovements(pdfText);
        }
        throw new BusinessException("El PDF no parece un extracto de cuenta ni de tarjeta de Santander");
    }

    private List<ParsedExpense> parseAccountMovements(String pdfText) {
        var currency = this.resolveCurrency();
        var result = new ArrayList<ParsedExpense>();

        LocalDate currentDate = null;
        var concept = new StringBuilder();

        for (String rawLine : pdfText.lines().toList()) {
            String line = rawLine.trim();
            if (ACCOUNT_DATE_LINE.matcher(line).matches()) {
                currentDate = LocalDate.parse(line, ACCOUNT_DATE_FORMAT);
                concept.setLength(0);
                continue;
            }
            if (currentDate == null || line.isEmpty() || line.startsWith(ACCOUNT_VALUE_DATE_PREFIX)
                    || ACCOUNT_PAGE_NOISE.matcher(line).matches()) {
                continue;
            }

            Matcher amountMatcher = ACCOUNT_AMOUNT_TAIL.matcher(line);
            if (!amountMatcher.find()) {
                concept.append(line).append(' ');
                continue;
            }

            concept.append(line, 0, amountMatcher.start());
            this.buildAccountExpense(currentDate, concept.toString(), amountMatcher.group(1), currency)
                    .ifPresent(result::add);
            currentDate = null;
            concept.setLength(0);
        }
        return result;
    }

    private Optional<ParsedExpense> buildAccountExpense(LocalDate date, String rawConcept, String rawAmount, Currency currency) {
        var amount = parseAmount(rawAmount);
        if (amount.isEmpty() || amount.get().signum() == 0) {
            log.warn("Movimiento de cuenta Santander sin importe válido: '{}'", rawConcept);
            return Optional.empty();
        }
        var type = amount.get().signum() < 0 ? MovementType.DEBITO : MovementType.INGRESO;
        return Optional.of(new ParsedExpense(
                date,
                this.cleanAccountConcept(rawConcept),
                currency,
                amount.get().abs(),
                type));
    }

    private List<ParsedExpense> parseCreditStatement(String pdfText) {
        var currency = this.resolveCurrency();
        return pdfText.lines()
                .map(String::trim)
                .map(CREDIT_LINE::matcher)
                .filter(Matcher::matches)
                .map(matcher -> this.buildCreditExpense(matcher, currency))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ParsedExpense> buildCreditExpense(Matcher matcher, Currency currency) {
        var amount = parseAmount(matcher.group(CREDIT_AMOUNT_POSITION));
        if (amount.isEmpty() || amount.get().signum() == 0) {
            log.warn("Línea de tarjeta Santander sin importe válido: '{}'", matcher.group(0));
            return Optional.empty();
        }
        var type = amount.get().signum() < 0 ? MovementType.REINTEGRO : MovementType.CREDITO;
        return Optional.of(new ParsedExpense(
                LocalDate.parse(matcher.group(CREDIT_DATE_POSITION), CREDIT_DATE_FORMAT),
                this.cleanCreditConcept(matcher.group(CREDIT_CONCEPT_POSITION)),
                currency,
                amount.get().abs(),
                type));
    }

    private String cleanAccountConcept(String concept) {
        String cleaned = MULTIPLE_SPACES.matcher(concept.trim()).replaceAll(" ");
        cleaned = ACCOUNT_CARD_SUFFIX.matcher(cleaned).replaceFirst("");
        cleaned = ACCOUNT_PREFIXES.matcher(cleaned).replaceFirst("");
        return this.stripTrailingSeparators(cleaned);
    }

    private String cleanCreditConcept(String concept) {
        String cleaned = MULTIPLE_SPACES.matcher(concept.trim()).replaceAll(" ");
        cleaned = CREDIT_PREFIXES.matcher(cleaned).replaceFirst("");
        return this.stripTrailingSeparators(cleaned);
    }

    private String stripTrailingSeparators(String value) {
        return value.replaceAll("[,\\s]+$", "").trim();
    }

    private Currency resolveCurrency() {
        return currencyRepository.findBySymbol(CURRENCY_SYMBOL)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found: " + CURRENCY_SYMBOL));
    }
}
