package pl.empik.task.empikservice.domain.exception;

import pl.empik.task.empikservice.domain.model.CouponCode;

public final class CouponNotFoundException extends DomainException {

    public CouponNotFoundException(CouponCode code) {
        super("coupon " + code + " does not exist");
    }
}
