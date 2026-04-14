package api.m2.movements.exceptions;

/**
 * Excepción lanzada cuando no se puede obtener la tasa de cambio para una moneda y fecha específica.
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String message) {
        super(message);
    }

    public ExchangeRateNotFoundException(String symbol, String date, Throwable cause) {
        super(String.format("No se pudo obtener la tasa de cambio para %s en %s", symbol, date), cause);
    }
}
