package pl.empik.task.empikservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    private GetCouponService service;

    @BeforeEach
    void setUp() {
        service = new GetCouponService(couponRepository);
    }

    @Test
    void findsCouponByNormalizedCode() {
        Coupon coupon = Coupon.reconstitute(
                CouponCode.of("WIOSNA"), Instant.parse("2026-08-26T12:00:00Z"), 5, 2, Country.of("PL"));
        when(couponRepository.findByCode(CouponCode.of("WIOSNA"))).thenReturn(Optional.of(coupon));

        assertThat(service.get("wiosna")).isSameAs(coupon);
    }

    @Test
    void throwsNotFoundForUnknownCode() {
        when(couponRepository.findByCode(CouponCode.of("NOPE"))).thenReturn(Optional.empty());

        assertThatExceptionOfType(CouponNotFoundException.class)
                .isThrownBy(() -> service.get("nope"))
                .withMessageContaining("NOPE");
    }
}
