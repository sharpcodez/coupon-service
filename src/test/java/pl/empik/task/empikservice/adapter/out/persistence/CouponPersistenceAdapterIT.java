package pl.empik.task.empikservice.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import pl.empik.task.empikservice.domain.exception.DuplicateCouponCodeException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.support.PostgresTestConfiguration;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJdbcTest
@Import({CouponPersistenceAdapter.class, PostgresTestConfiguration.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class CouponPersistenceAdapterIT {

    private static final Instant CREATED_AT = Instant.parse("2026-08-26T12:00:00Z");

    @Autowired
    private CouponPersistenceAdapter adapter;

    @Test
    void savesAndReloadsCouponWithAllAttributes() {
        Coupon saved = adapter.save(Coupon.create(CouponCode.of("wiosna"), 5, Country.of("PL"), CREATED_AT));

        assertThat(saved.code().value()).isEqualTo("WIOSNA");

        Optional<Coupon> reloaded = adapter.findByCode(CouponCode.of("WIOSNA"));
        assertThat(reloaded).hasValueSatisfying(coupon -> {
            assertThat(coupon.code().value()).isEqualTo("WIOSNA");
            assertThat(coupon.createdAt()).isEqualTo(CREATED_AT);
            assertThat(coupon.maxUsages()).isEqualTo(5);
            assertThat(coupon.currentUsages()).isZero();
            assertThat(coupon.country()).isEqualTo(Country.of("PL"));
        });
    }

    @Test
    void lookupIsCaseInsensitiveViaNormalizedCode() {
        adapter.save(Coupon.create(CouponCode.of("LATO"), 1, Country.of("PL"), CREATED_AT));

        assertThat(adapter.findByCode(CouponCode.of("lato"))).isPresent();
        assertThat(adapter.findByCode(CouponCode.of("LaTo"))).isPresent();
    }

    @Test
    void findByUnknownCodeReturnsEmpty() {
        assertThat(adapter.findByCode(CouponCode.of("GHOST"))).isEmpty();
    }

    @Test
    void duplicateCodeIsRejectedCaseInsensitively() {
        adapter.save(Coupon.create(CouponCode.of("ZIMA"), 1, Country.of("PL"), CREATED_AT));

        assertThatExceptionOfType(DuplicateCouponCodeException.class)
                .isThrownBy(() -> adapter.save(
                        Coupon.create(CouponCode.of("zima"), 9, Country.of("DE"), CREATED_AT)))
                .withMessageContaining("ZIMA");
    }
}
