package pl.empik.task.empikservice.adapter.out.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.empik.task.empikservice.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.model.UserId;
import pl.empik.task.empikservice.support.PostgresTestConfiguration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJdbcTest
@Import({CouponPersistenceAdapter.class, PostgresTestConfiguration.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RecordRedemptionIT {

    private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");
    private static final CouponCode CODE = CouponCode.of("WIOSNA");
    private static final UserId ALICE = UserId.of("alice");
    private static final UserId BOB = UserId.of("bob");

    @Autowired
    private CouponPersistenceAdapter adapter;
    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    @AfterEach
    void cleanTables() {
        jdbcClient.sql("DELETE FROM redemption").update();
        jdbcClient.sql("DELETE FROM coupon").update();
    }

    private void couponWith(int maxUsages) {
        adapter.save(Coupon.create(CODE, maxUsages, Country.of("PL"), NOW));
    }

    private long redemptionCount() {
        return jdbcClient.sql("SELECT count(*) FROM redemption").query(Long.class).single();
    }

    private int currentUsages() {
        return jdbcClient.sql("SELECT current_usages FROM coupon WHERE code = :code")
                .param("code", CODE.value()).query(Integer.class).single();
    }

    @Test
    void recordsRedemptionAndIncrementsUsageAtomically() {
        couponWith(2);

        Redemption redemption = adapter.recordRedemption(CODE, ALICE, NOW);

        assertThat(redemption).isEqualTo(new Redemption(CODE, ALICE, NOW));
        assertThat(currentUsages()).isEqualTo(1);
        assertThat(redemptionCount()).isEqualTo(1);
    }

    @Test
    void secondRedemptionBySameUserIsRejected() {
        couponWith(5);
        adapter.recordRedemption(CODE, ALICE, NOW);

        assertThatExceptionOfType(CouponAlreadyRedeemedException.class)
                .isThrownBy(() -> adapter.recordRedemption(CODE, ALICE, NOW.plusSeconds(1)))
                .withMessageContaining("WIOSNA").withMessageContaining("alice");
        assertThat(currentUsages()).isEqualTo(1);
        assertThat(redemptionCount()).isEqualTo(1);
    }

    @Test
    void exhaustedCouponRejectsAndRollsBackTheRedemptionRow() {
        couponWith(1);
        adapter.recordRedemption(CODE, ALICE, NOW);

        assertThatExceptionOfType(CouponExhaustedException.class)
                .isThrownBy(() -> adapter.recordRedemption(CODE, BOB, NOW.plusSeconds(1)));

        assertThat(redemptionCount()).isEqualTo(1);
        assertThat(currentUsages()).isEqualTo(1);
    }

    @Test
    void unknownCouponThrowsNotFoundAndWritesNothing() {
        assertThatExceptionOfType(CouponNotFoundException.class)
                .isThrownBy(() -> adapter.recordRedemption(CouponCode.of("GHOST"), ALICE, NOW));
        assertThat(redemptionCount()).isZero();
    }
}
