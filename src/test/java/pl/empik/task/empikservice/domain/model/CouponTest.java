package pl.empik.task.empikservice.domain.model;

import org.junit.jupiter.api.Test;
import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.exception.CouponNotValidInCountryException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CouponTest {

    private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");
    private static final Country PL = Country.of("PL");
    private static final Country DE = Country.of("DE");

    @Test
    void createStartsWithZeroUsagesAndGivenAttributes() {
        Coupon coupon = Coupon.create(CouponCode.of("wiosna"), 3, PL, NOW);

        assertThat(coupon.code().value()).isEqualTo("WIOSNA");
        assertThat(coupon.createdAt()).isEqualTo(NOW);
        assertThat(coupon.maxUsages()).isEqualTo(3);
        assertThat(coupon.currentUsages()).isZero();
        assertThat(coupon.country()).isEqualTo(PL);
    }

    @Test
    void createRejectsNonPositiveMaxUsages() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Coupon.create(CouponCode.of("X"), 0, PL, NOW));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Coupon.create(CouponCode.of("X"), -1, PL, NOW));
    }

    @Test
    void reconstituteRejectsUsagesOutsideBounds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Coupon.reconstitute(CouponCode.of("X"), NOW, 3, 4, PL));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Coupon.reconstitute(CouponCode.of("X"), NOW, 3, -1, PL));
    }

    @Test
    void redeemableFromMatchingCountryWhenUsagesRemain() {
        Coupon coupon = Coupon.reconstitute(CouponCode.of("X"), NOW, 3, 2, PL);
        assertThatCode(() -> coupon.ensureRedeemableFrom(PL)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRedemptionFromOtherCountry() {
        Coupon coupon = Coupon.create(CouponCode.of("X"), 3, PL, NOW);
        assertThatExceptionOfType(CouponNotValidInCountryException.class)
                .isThrownBy(() -> coupon.ensureRedeemableFrom(DE))
                .withMessageContaining("X").withMessageContaining("DE");
    }

    @Test
    void rejectsRedemptionWhenExhausted() {
        Coupon coupon = Coupon.reconstitute(CouponCode.of("X"), NOW, 3, 3, PL);
        assertThat(coupon.isExhausted()).isTrue();
        assertThatExceptionOfType(CouponExhaustedException.class)
                .isThrownBy(() -> coupon.ensureRedeemableFrom(PL));
    }

    @Test
    void countryCheckTakesPrecedenceOverExhaustion() {
        Coupon coupon = Coupon.reconstitute(CouponCode.of("X"), NOW, 1, 1, PL);
        assertThatExceptionOfType(CouponNotValidInCountryException.class)
                .isThrownBy(() -> coupon.ensureRedeemableFrom(DE));
    }

    @Test
    void identityIsTheCode() {
        Coupon a = Coupon.create(CouponCode.of("SAME"), 1, PL, NOW);
        Coupon b = Coupon.reconstitute(CouponCode.of("same"), NOW.plusSeconds(60), 9, 5, DE);
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void notEqualToCouponWithADifferentCodeOrToAnUnrelatedType() {
        Coupon a = Coupon.create(CouponCode.of("A"), 1, PL, NOW);
        Coupon b = Coupon.create(CouponCode.of("B"), 1, PL, NOW);

        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo("A");
        assertThat(a.equals(null)).isFalse();
    }

    @Test
    void hashCodeDelegatesToTheCode() {
        Coupon coupon = Coupon.create(CouponCode.of("wiosna"), 3, PL, NOW);
        assertThat(coupon.hashCode()).isEqualTo(coupon.code().hashCode());
    }

    @Test
    void reconstituteExposesTheGivenCurrentUsages() {
        Coupon coupon = Coupon.reconstitute(CouponCode.of("X"), NOW, 3, 2, PL);
        assertThat(coupon.currentUsages()).isEqualTo(2);
    }
}
