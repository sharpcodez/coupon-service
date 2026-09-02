package pl.empik.task.empikservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.empik.task.empikservice.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase.CreateCouponCommand;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase.RedeemCouponCommand;
import pl.empik.task.empikservice.domain.port.out.GeoLocationProvider;
import pl.empik.task.empikservice.support.PostgresTestConfiguration;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.security.jwt.secret=integration-test-secret-0123456789abcdef")
@Import(PostgresTestConfiguration.class)
class ConcurrentRedemptionIT {

    @Autowired
    private CreateCouponUseCase createCoupon;
    @Autowired
    private RedeemCouponUseCase redeemCoupon;
    @Autowired
    private JdbcClient jdbcClient;
    @MockitoBean
    private GeoLocationProvider geoLocationProvider;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM redemption").update();
        jdbcClient.sql("DELETE FROM coupon").update();
        when(geoLocationProvider.resolveCountry(any())).thenReturn(Country.of("PL"));
    }

    private record Outcome(int successes, int exhausted, int alreadyRedeemed) {}

    private Outcome stampede(int attempts, IntFunction<RedeemCouponCommand> command) throws Exception {
        CountDownLatch startGun = new CountDownLatch(1);
        List<Callable<Redemption>> tasks = IntStream.range(0, attempts)
                .mapToObj(i -> (Callable<Redemption>) () -> {
                    startGun.await();
                    return redeemCoupon.redeem(command.apply(i));
                })
                .toList();

        List<Future<Redemption>> futures;
        try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<Redemption>> submitted = tasks.stream().map(executor::submit).toList();
            startGun.countDown();
            futures = submitted;
        }

        int successes = 0;
        int exhausted = 0;
        int alreadyRedeemed = 0;
        for (Future<Redemption> future : futures) {
            try {
                future.get();
                successes++;
            } catch (ExecutionException e) {
                switch (e.getCause()) {
                    case CouponExhaustedException ignored -> exhausted++;
                    case CouponAlreadyRedeemedException ignored -> alreadyRedeemed++;
                    default -> throw e;
                }
            }
        }
        return new Outcome(successes, exhausted, alreadyRedeemed);
    }

    private int currentUsages(String code) {
        return jdbcClient.sql("SELECT current_usages FROM coupon WHERE code = :code")
                .param("code", code).query(Integer.class).single();
    }

    private long redemptionCount() {
        return jdbcClient.sql("SELECT count(*) FROM redemption").query(Long.class).single();
    }

    @Test
    void exactlyMaxUsagesWinnersUnderHeavyContention() throws Exception {
        createCoupon.create(new CreateCouponCommand("RACE", 5, "PL"));

        Outcome outcome = stampede(50, i -> new RedeemCouponCommand("RACE", "user-" + i, "8.8.8.8"));

        assertThat(outcome.successes()).isEqualTo(5);
        assertThat(outcome.exhausted()).isEqualTo(45);
        assertThat(outcome.alreadyRedeemed()).isZero();
        assertThat(currentUsages("RACE")).isEqualTo(5);
        assertThat(redemptionCount()).isEqualTo(5);
    }

    @Test
    void sameUserWinsAtMostOnceUnderContention() throws Exception {
        createCoupon.create(new CreateCouponCommand("ONCE", 10, "PL"));

        Outcome outcome = stampede(20, i -> new RedeemCouponCommand("ONCE", "greedy-user", "8.8.8.8"));

        assertThat(outcome.successes()).isEqualTo(1);
        assertThat(outcome.alreadyRedeemed()).isEqualTo(19);
        assertThat(outcome.exhausted()).isZero();
        assertThat(currentUsages("ONCE")).isEqualTo(1);
        assertThat(redemptionCount()).isEqualTo(1);
    }
}
