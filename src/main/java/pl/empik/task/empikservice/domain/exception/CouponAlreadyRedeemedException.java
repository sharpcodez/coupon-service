package pl.empik.task.empikservice.domain.exception;

import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.model.UserId;

public final class CouponAlreadyRedeemedException extends DomainException {

    public CouponAlreadyRedeemedException(CouponCode code, UserId userId, Throwable cause) {
        super("coupon " + code + " was already redeemed by user " + userId, cause);
    }
}
