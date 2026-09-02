package pl.empik.task.empikservice.domain.exception;

import pl.empik.task.empikservice.domain.model.CouponCode;

public final class DuplicateCouponCodeException extends DomainException {

    public DuplicateCouponCodeException(CouponCode code, Throwable cause) {
        super("coupon " + code + " already exists", cause);
    }
}
