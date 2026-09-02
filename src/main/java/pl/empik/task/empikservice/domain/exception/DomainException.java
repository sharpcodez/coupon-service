package pl.empik.task.empikservice.domain.exception;

public abstract sealed class DomainException extends RuntimeException
        permits CouponNotFoundException, CouponExhaustedException, CouponAlreadyRedeemedException,
        CouponNotValidInCountryException, DuplicateCouponCodeException,
        GeoLocationUnavailableException, CountryUnresolvableException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
