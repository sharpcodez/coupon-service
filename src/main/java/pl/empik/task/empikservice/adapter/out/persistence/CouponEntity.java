package pl.empik.task.empikservice.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;

import java.time.Instant;

@Table("coupon")
record CouponEntity(
        @Id Long id,
        String code,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        String country) {

    static CouponEntity fromDomain(Coupon coupon) {
        return new CouponEntity(
                null,
                coupon.code().value(),
                coupon.createdAt(),
                coupon.maxUsages(),
                coupon.currentUsages(),
                coupon.country().isoCode());
    }

    Coupon toDomain() {
        return Coupon.reconstitute(
                CouponCode.of(code), createdAt, maxUsages, currentUsages, Country.of(country));
    }
}
