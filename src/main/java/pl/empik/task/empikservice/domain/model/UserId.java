package pl.empik.task.empikservice.domain.model;

import java.util.Objects;

public record UserId(String value) {

    public static final int MAX_LENGTH = 128;

    public UserId {
        Objects.requireNonNull(value, "user id must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("user id must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("user id must not exceed " + MAX_LENGTH + " characters");
        }
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
