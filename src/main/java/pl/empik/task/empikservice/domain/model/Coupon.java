package pl.empik.task.empikservice.domain.model;

import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.exception.CouponNotValidInCountryException;

import java.time.Instant;
import java.util.Objects;

public final class Coupon {

    private final CouponCode code;
    private final Instant createdAt;
    private final int maxUsages;
    private final int currentUsages;
    private final Country country;

    private Coupon(CouponCode code, Instant createdAt, int maxUsages, int currentUsages, Country country) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.country = Objects.requireNonNull(country, "country must not be null");
        if (maxUsages < 1) {
            throw new IllegalArgumentException("maxUsages must be at least 1");
        }
        if (currentUsages < 0 || currentUsages > maxUsages) {
            throw new IllegalArgumentException("currentUsages must be between 0 and maxUsages");
        }
        this.maxUsages = maxUsages;
        this.currentUsages = currentUsages;
    }

    public static Coupon create(CouponCode code, int maxUsages, Country country, Instant now) {
        return new Coupon(code, now, maxUsages, 0, country);
    }

    public static Coupon reconstitute(CouponCode code, Instant createdAt, int maxUsages,
                                      int currentUsages, Country country) {
        return new Coupon(code, createdAt, maxUsages, currentUsages, country);
    }

    public void ensureRedeemableFrom(Country requestCountry) {
        if (!country.equals(requestCountry)) {
            throw new CouponNotValidInCountryException(code, requestCountry);
        }
        if (isExhausted()) {
            throw new CouponExhaustedException(code);
        }
    }

    public boolean isExhausted() {
        return currentUsages >= maxUsages;
    }

    public CouponCode code() {
        return code;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public int maxUsages() {
        return maxUsages;
    }

    public int currentUsages() {
        return currentUsages;
    }

    public Country country() {
        return country;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Coupon other && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
