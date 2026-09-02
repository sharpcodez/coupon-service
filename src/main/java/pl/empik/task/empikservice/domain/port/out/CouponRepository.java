package pl.empik.task.empikservice.domain.port.out;

import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.model.UserId;

import java.time.Instant;
import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> findByCode(CouponCode code);

    Coupon save(Coupon coupon);

    Redemption recordRedemption(CouponCode code, UserId userId, Instant redeemedAt);
}
