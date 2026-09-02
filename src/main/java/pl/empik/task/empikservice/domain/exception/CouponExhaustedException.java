package pl.empik.task.empikservice.domain.exception;

import pl.empik.task.empikservice.domain.model.CouponCode;

public final class CouponExhaustedException extends DomainException {

    public CouponExhaustedException(CouponCode code) {
        super("coupon " + code + " has reached its maximum number of usages");
    }
}
