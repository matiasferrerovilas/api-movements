package api.m2.movements.exceptions;

public final class ServiceException extends DomainException {
    public ServiceException(String message) {
        super(message);
    }
}
