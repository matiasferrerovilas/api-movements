package api.m2.movements.helpers;

import api.m2.movements.entities.Currency;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.records.pdf.ParsedExpense;
import api.m2.movements.repositories.CurrencyRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Extractor de movimientos desde archivos Excel/CSV del banco Santander.
 *
 * Formato esperado:
 * - Columnas: FECHA OPERACIÓN | FECHA VALOR | CONCEPTO | IMPORTE | SALDO
 * - Fecha: formato dd/MM/yyyy (ej: 13/04/2026)
 * - Importe: formato español -1.234,56 EUR (negativos = gastos, positivos = ingresos)
 * - Primera fila contiene headers (se ignora)
 * - Se usa FECHA OPERACIÓN para la fecha del movimiento
 */
@Component
@Slf4j
public class SantanderExcelExtractorHelper extends ExcelExtractorHelper {

    private static final int COL_FECHA_OPERACION = 0;
    private static final int COL_FECHA_VALOR = 1;
    private static final int COL_CONCEPTO = 2;
    private static final int COL_IMPORTE = 3;
    private static final int COL_SALDO = 4;
    private static final int REQUIRED_COLUMNS = 5;
    private static final int MIN_COLUMNS_FOR_PARSING = 3;
    private static final int MAX_DESCRIPTION_LENGTH = 60;
    private static final int XLS_MAGIC_BYTES_LENGTH = 4;
    private static final int XLSX_MAGIC_BYTES_LENGTH = 2;
    private static final int BYTE_MASK = 0xFF;
    private static final int XLS_BYTE_0 = 0xD0;
    private static final int XLS_BYTE_1 = 0xCF;
    private static final int XLS_BYTE_2 = 0x11;
    private static final int XLS_BYTE_3 = 0xE0;
    private static final int XLSX_BYTE_0 = 0x50;
    private static final int XLSX_BYTE_1 = 0x4B;
    private static final int BYTE_INDEX_0 = 0;
    private static final int BYTE_INDEX_1 = 1;
    private static final int BYTE_INDEX_2 = 2;
    private static final int BYTE_INDEX_3 = 3;

    public SantanderExcelExtractorHelper(CurrencyRepository currencyRepository) {
        super(currencyRepository);
    }

    @Override
    public String getBank() {
        return "SANTANDER";
    }

    @Override
    public List<ParsedExpense> parse(byte[] fileContent) {
        // Detectar tipo de archivo por magic bytes
        if (this.isXlsFile(fileContent)) {
            return this.parseXls(fileContent);
        } else if (this.isXlsxFile(fileContent)) {
            return this.parseXlsx(fileContent);
        } else {
            // Asumir CSV si no es Excel
            return this.parseCsv(fileContent);
        }
    }

    private boolean isXlsFile(byte[] content) {
        // XLS (Excel 97-2003) comienza con magic bytes D0 CF 11 E0
        return content.length >= XLS_MAGIC_BYTES_LENGTH
                && (content[BYTE_INDEX_0] & BYTE_MASK) == XLS_BYTE_0
                && (content[BYTE_INDEX_1] & BYTE_MASK) == XLS_BYTE_1
                && (content[BYTE_INDEX_2] & BYTE_MASK) == XLS_BYTE_2
                && (content[BYTE_INDEX_3] & BYTE_MASK) == XLS_BYTE_3;
    }

    private boolean isXlsxFile(byte[] content) {
        // XLSX es un ZIP que comienza con PK (0x50 0x4B)
        return content.length >= XLSX_MAGIC_BYTES_LENGTH
                && (content[BYTE_INDEX_0] & BYTE_MASK) == XLSX_BYTE_0
                && (content[BYTE_INDEX_1] & BYTE_MASK) == XLSX_BYTE_1;
    }

    private List<ParsedExpense> parseXls(byte[] fileContent) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileContent);
             Workbook workbook = new HSSFWorkbook(bis)) {
            return this.extractFromWorkbook(workbook);
        } catch (IOException e) {
            log.error("Error parsing XLS file", e);
            throw new BusinessException("Error al procesar archivo XLS: " + e.getMessage());
        }
    }

    private List<ParsedExpense> parseXlsx(byte[] fileContent) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileContent);
             Workbook workbook = new XSSFWorkbook(bis)) {
            return this.extractFromWorkbook(workbook);
        } catch (IOException e) {
            log.error("Error parsing XLSX file", e);
            throw new BusinessException("Error al procesar archivo XLSX: " + e.getMessage());
        }
    }

    private List<ParsedExpense> parseCsv(byte[] fileContent) {
        try (InputStreamReader reader = new InputStreamReader(
                new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8)) {

            // Configurar CSVReader con punto y coma como separador (formato español)
            com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder()
                    .withSeparator(';')
                    .build();

            try (CSVReader csvReader = new com.opencsv.CSVReaderBuilder(reader)
                    .withCSVParser(parser)
                    .build()) {

                List<String[]> rows = csvReader.readAll();
                List<ParsedExpense> expenses = new ArrayList<>();

                // Ignorar header (primera fila)
                for (int i = 1; i < rows.size(); i++) {
                    String[] row = rows.get(i);
                    this.parseRow(row).ifPresent(expenses::add);
                }

                log.info("Parsed {} expenses from CSV for bank {}", expenses.size(), this.getBank());
                return expenses;
            }

        } catch (IOException | CsvException e) {
            log.error("Error parsing CSV file", e);
            throw new BusinessException("Error al procesar archivo CSV: " + e.getMessage());
        }
    }

    private List<ParsedExpense> extractFromWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0); // primera hoja
        List<ParsedExpense> expenses = new ArrayList<>();

        // Ignorar header (primera fila = índice 0)
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String[] cells = new String[REQUIRED_COLUMNS];
            cells[COL_FECHA_OPERACION] = this.getCellValueAsString(row.getCell(COL_FECHA_OPERACION));
            cells[COL_FECHA_VALOR] = this.getCellValueAsString(row.getCell(COL_FECHA_VALOR));
            cells[COL_CONCEPTO] = this.getCellValueAsString(row.getCell(COL_CONCEPTO));
            cells[COL_IMPORTE] = this.getCellValueAsString(row.getCell(COL_IMPORTE));
            cells[COL_SALDO] = this.getCellValueAsString(row.getCell(COL_SALDO));

            this.parseRow(cells).ifPresent(expenses::add);
        }

        log.info("Parsed {} expenses from Excel for bank {}", expenses.size(), this.getBank());
        return expenses;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private java.util.Optional<ParsedExpense> parseRow(String[] cells) {
        if (cells.length < MIN_COLUMNS_FOR_PARSING) {
            log.debug("Skipping row with insufficient columns: {}", cells.length);
            return java.util.Optional.empty();
        }

        String fechaStr = cells[COL_FECHA_OPERACION];
        String concepto = cells[COL_CONCEPTO];
        String importeStr = cells[COL_IMPORTE];

        // Validar campos obligatorios
        if (fechaStr == null || fechaStr.trim().isEmpty()
                || concepto == null || concepto.trim().isEmpty()
                || importeStr == null || importeStr.trim().isEmpty()) {
            log.debug("Skipping row with empty required fields");
            return java.util.Optional.empty();
        }

        // Parsear fecha
        var dateOpt = this.parseDate(fechaStr);
        if (dateOpt.isEmpty()) {
            log.warn("Failed to parse date for row: {}", fechaStr);
            return java.util.Optional.empty();
        }

        // Extraer moneda del importe (ej: "-123,45 EUR" -> "EUR")
        String currencySymbol = this.extractCurrency(importeStr);

        // Limpiar importe (eliminar moneda)
        String cleanImporte = importeStr.replaceAll("[A-Z]{3}", "").trim();

        // Parsear importe
        var amountOpt = this.parseAmount(cleanImporte);
        if (amountOpt.isEmpty()) {
            log.warn("Failed to parse amount for row: {}", importeStr);
            return java.util.Optional.empty();
        }

        // Buscar currency entity
        Currency currency = currencyRepository.findBySymbol(currencySymbol)
                .orElseGet(() -> {
                    log.warn("Currency not found: {}, using EUR as default", currencySymbol);
                    return currencyRepository.findBySymbol("EUR")
                            .orElseThrow(() -> new BusinessException("Moneda EUR no encontrada en base de datos"));
                });

        // Truncar concepto si excede el límite de la BD
        String truncatedConcepto = concepto.trim();
        if (truncatedConcepto.length() > MAX_DESCRIPTION_LENGTH) {
            truncatedConcepto = truncatedConcepto.substring(0, MAX_DESCRIPTION_LENGTH);
            log.debug("Truncated description from {} to {} chars", concepto.trim().length(), MAX_DESCRIPTION_LENGTH);
        }

        return java.util.Optional.of(new ParsedExpense(
                dateOpt.get(),
                truncatedConcepto,
                null,  // installment (no aplica para Santander)
                null,  // comprobante (no aplica)
                currency,
                amountOpt.get(),  // amountPesos (se usa para todos)
                BigDecimal.ZERO   // amountDolares (no aplica)
        ));
    }
}
