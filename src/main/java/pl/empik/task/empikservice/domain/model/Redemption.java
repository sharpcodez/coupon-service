package pl.empik.task.empikservice.domain.model;

import java.time.Instant;
import java.util.Objects;

public record Redemption(CouponCode couponCode, UserId userId, Instant redeemedAt) {

    public Redemption {
        Objects.requireNonNull(couponCode, "couponCode must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(redeemedAt, "redeemedAt must not be null");
    }
}
