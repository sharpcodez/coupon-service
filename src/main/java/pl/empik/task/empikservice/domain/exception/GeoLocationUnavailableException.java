package pl.empik.task.empikservice.domain.exception;

public final class GeoLocationUnavailableException extends DomainException {

    public GeoLocationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
