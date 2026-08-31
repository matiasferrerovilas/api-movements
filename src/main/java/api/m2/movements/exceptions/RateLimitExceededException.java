package api.m2.movements.exceptions;

// No extiende DomainException a propósito: esa jerarquía es para errores de dominio de negocio —
// esta es una excepción de infraestructura (protección contra abuso), como AuthenticationException
// o DataIntegrityViolationException, que ErrorHandler ya maneja por fuera de esa jerarquía. Mismo
// diseño que en api-identity.
public final class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
