package api.m2.movements.helpers;

import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.repositories.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper abstracto para extraer datos de archivos Excel/CSV bancarios.
 * Similar a PdfExtractorHelper pero para formatos estructurados.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public abstract class ExcelExtractorHelper {

    protected final CurrencyRepository currencyRepository;

    protected static final Pattern CURRENCY_PATTERN = Pattern.compile("(CHF|USD|EUR|GBP|ARS)");

    /**
     * Retorna el código del banco asociado a este extractor.
     *
     * @return código del banco (ej: "SANTANDER")
     */
    public abstract String getBank();

    /**
     * Parsea el contenido binario del archivo Excel/CSV y extrae los movimientos.
     *
     * @param fileContent contenido binario del archivo
     * @return lista de movimientos parseados
     */
    public abstract List<ParsedExpense> parse(byte[] fileContent);

    /**
     * Formato de fecha esperado por defecto: dd/MM/yyyy.
     * Puede ser sobreescrito por implementaciones específicas.
     *
     * @return formateador de fecha
     */
    protected DateTimeFormatter getDateFormat() {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy");
    }

    /**
     * Parsea una cadena de dinero en formato español a BigDecimal.
     * Formato esperado: -1.234,56 o 1.234,56
     * - Punto (.) como separador de miles
     * - Coma (,) como separador decimal
     *
     * @param amount cadena con el monto
     * @return BigDecimal parseado
     */
    protected BigDecimal parseMoney(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return null;
        }

        String trimmed = amount.trim();
        boolean hasComma = trimmed.contains(",");
        boolean hasDot = trimmed.contains(".");

        String cleaned;
        if (hasComma && hasDot) {
            // Formato español: 1.234,56 → punto=miles, coma=decimal
            cleaned = trimmed.replace(".", "").replace(",", ".");
        } else if (hasDot) {
            // Formato numérico (Java/Excel): 8.9 → punto=decimal (ya está OK)
            cleaned = trimmed;
        } else if (hasComma) {
            // Formato español sin miles: 8,9 → coma=decimal
            cleaned = trimmed.replace(",", ".");
        } else {
            // Solo dígitos, sin separador decimal
            cleaned = trimmed;
        }

        return new BigDecimal(cleaned);
    }

    /**
     * Intenta parsear un monto, retornando Optional.empty() si falla.
     *
     * @param amountStr cadena con el monto
     * @return Optional con el BigDecimal parseado, o empty si falla
     */
    protected Optional<BigDecimal> parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(this.parseMoney(amountStr.trim()));
        } catch (Exception e) {
            log.debug("Failed to parse amount: '{}'", amountStr);
            return Optional.empty();
        }
    }

    /**
     * Parsea una fecha en el formato especificado.
     *
     * @param dateStr cadena con la fecha
     * @return LocalDate parseado
     */
    protected Optional<LocalDate> parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(dateStr.trim(), this.getDateFormat()));
        } catch (Exception e) {
            log.debug("Failed to parse date: '{}'", dateStr);
            return Optional.empty();
        }
    }

    /**
     * Extrae el símbolo de moneda de una cadena.
     * Busca patrones como EUR, USD, GBP, etc.
     *
     * @param text texto que contiene la moneda
     * @return símbolo de moneda o "EUR" por defecto
     */
    protected String extractCurrency(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "EUR"; // moneda por defecto
        }

        Matcher matcher = CURRENCY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "EUR"; // moneda por defecto si no se encuentra
    }
}
