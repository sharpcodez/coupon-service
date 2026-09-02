package pl.empik.task.empikservice.domain.exception;

import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.CouponCode;

public final class CouponNotValidInCountryException extends DomainException {

    public CouponNotValidInCountryException(CouponCode code, Country requestCountry) {
        super("coupon " + code + " cannot be redeemed from country " + requestCountry);
    }
}
