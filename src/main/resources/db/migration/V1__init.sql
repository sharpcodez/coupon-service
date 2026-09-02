CREATE TABLE coupon
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code           VARCHAR(64)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    max_usages     INTEGER      NOT NULL CHECK (max_usages >= 1),
    current_usages INTEGER      NOT NULL DEFAULT 0,
    country        CHAR(2)      NOT NULL,
    CONSTRAINT chk_coupon_usages CHECK (current_usages >= 0 AND current_usages <= max_usages)
);

-- codes are stored already normalized (uppercase) by the domain, so a plain unique index
-- delivers case-insensitive uniqueness
CREATE UNIQUE INDEX ux_coupon_code ON coupon (code);

CREATE TABLE redemption
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    coupon_id   BIGINT       NOT NULL REFERENCES coupon (id),
    user_id     VARCHAR(128) NOT NULL,
    redeemed_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_redemption_coupon_user UNIQUE (coupon_id, user_id)
);
