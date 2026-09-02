package pl.empik.task.empikservice.adapter.in.rest.dto;

import pl.empik.task.empikservice.domain.model.Coupon;

import java.time.Instant;

public record CouponResponse(String code, Instant createdAt, int maxUsages,
                             int currentUsages, String country) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(coupon.code().value(), coupon.createdAt(),
                coupon.maxUsages(), coupon.currentUsages(), coupon.country().isoCode());
    }
}
