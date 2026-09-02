package pl.empik.task.empikservice.domain.model;

import java.util.Locale;
import java.util.Objects;

public record CouponCode(String value) {

    public static final int MAX_LENGTH = 64;

    public CouponCode {
        Objects.requireNonNull(value, "coupon code must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("coupon code must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "coupon code must not exceed " + MAX_LENGTH + " characters");
        }
        if (!value.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "coupon code may contain only letters, digits, '-' and '_'");
        }
    }

    public static CouponCode of(String value) {
        return new CouponCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
