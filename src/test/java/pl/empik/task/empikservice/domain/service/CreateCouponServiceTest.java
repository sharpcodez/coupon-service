package pl.empik.task.empikservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase.CreateCouponCommand;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCouponServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-26T12:00:00Z");

    @Mock
    private CouponRepository couponRepository;

    private CreateCouponService service;

    @BeforeEach
    void setUp() {
        service = new CreateCouponService(couponRepository, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void createsNormalizedCouponStampedWithClockTime() {
        when(couponRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Coupon coupon = service.create(new CreateCouponCommand("wiosna", 5, "pl"));

        assertThat(coupon.code().value()).isEqualTo("WIOSNA");
        assertThat(coupon.createdAt()).isEqualTo(FIXED_NOW);
        assertThat(coupon.maxUsages()).isEqualTo(5);
        assertThat(coupon.currentUsages()).isZero();
        assertThat(coupon.country()).isEqualTo(Country.of("PL"));
        verify(couponRepository).save(coupon);
    }

    @Test
    void returnsThePersistedAggregate() {
        Coupon persisted = Coupon.reconstitute(CouponCode.of("WIOSNA"), FIXED_NOW, 5, 0, Country.of("PL"));
        when(couponRepository.save(any())).thenReturn(persisted);

        assertThat(service.create(new CreateCouponCommand("WIOSNA", 5, "PL"))).isSameAs(persisted);
    }

    @Test
    void rejectsInvalidCommandWithoutTouchingTheRepository() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.create(new CreateCouponCommand(" ", 5, "PL")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.create(new CreateCouponCommand("X", 5, "ZZ")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.create(new CreateCouponCommand("X", 0, "PL")));
        verifyNoInteractions(couponRepository);
    }
}
