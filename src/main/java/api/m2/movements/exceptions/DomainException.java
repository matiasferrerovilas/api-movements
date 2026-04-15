package api.m2.movements.exceptions;

/**
 * Clase base sellada para todas las excepciones de dominio del sistema.
 * Permite un manejo exhaustivo de excepciones en switch expressions.
 */
public sealed class DomainException extends RuntimeException
        permits BusinessException, EntityNotFoundException, PermissionDeniedException, ExchangeRateNotFoundException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
