package api.m2.movements.exceptions;

public final class PermissionDeniedException extends DomainException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
