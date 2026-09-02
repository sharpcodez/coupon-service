package pl.empik.task.empikservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.exception.CouponNotValidInCountryException;
import pl.empik.task.empikservice.domain.exception.GeoLocationUnavailableException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.model.IpAddress;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.model.UserId;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase.RedeemCouponCommand;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;
import pl.empik.task.empikservice.domain.port.out.GeoLocationProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedeemCouponServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-26T12:00:00Z");
    private static final CouponCode CODE = CouponCode.of("WIOSNA");
    private static final UserId USER = UserId.of("alice");
    private static final IpAddress IP = IpAddress.of("8.8.8.8");
    private static final Country PL = Country.of("PL");
    private static final Country DE = Country.of("DE");

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private GeoLocationProvider geoLocationProvider;

    private RedeemCouponService service;

    @BeforeEach
    void setUp() {
        service = new RedeemCouponService(
                couponRepository, geoLocationProvider, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private Coupon coupon(int maxUsages, int currentUsages) {
        return Coupon.reconstitute(CODE, FIXED_NOW.minusSeconds(3600), maxUsages, currentUsages, PL);
    }

    @Test
    void redeemsInOrderLookupThenGeoThenRecord() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 0)));
        when(geoLocationProvider.resolveCountry(IP)).thenReturn(PL);
        Redemption expected = new Redemption(CODE, USER, FIXED_NOW);
        when(couponRepository.recordRedemption(CODE, USER, FIXED_NOW)).thenReturn(expected);

        Redemption redemption = service.redeem(new RedeemCouponCommand("wiosna", "alice", "8.8.8.8"));

        assertThat(redemption).isSameAs(expected);
        InOrder order = inOrder(couponRepository, geoLocationProvider);
        order.verify(couponRepository).findByCode(CODE);
        order.verify(geoLocationProvider).resolveCountry(IP);
        order.verify(couponRepository).recordRedemption(CODE, USER, FIXED_NOW);
    }

    @Test
    void unknownCouponFailsWithoutSpendingAGeoLookup() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.empty());

        assertThatExceptionOfType(CouponNotFoundException.class)
                .isThrownBy(() -> service.redeem(new RedeemCouponCommand("WIOSNA", "alice", "8.8.8.8")));
        verifyNoInteractions(geoLocationProvider);
        verify(couponRepository, never()).recordRedemption(any(), any(), any());
    }

    @Test
    void wrongCountryIsRejectedBeforeAnyWrite() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 0)));
        when(geoLocationProvider.resolveCountry(IP)).thenReturn(DE);

        assertThatExceptionOfType(CouponNotValidInCountryException.class)
                .isThrownBy(() -> service.redeem(new RedeemCouponCommand("WIOSNA", "alice", "8.8.8.8")));
        verify(couponRepository, never()).recordRedemption(any(), any(), any());
    }

    @Test
    void exhaustedCouponFastFailsBeforeAnyWrite() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(coupon(3, 3)));
        when(geoLocationProvider.resolveCountry(IP)).thenReturn(PL);

        assertThatExceptionOfType(CouponExhaustedException.class)
                .isThrownBy(() -> service.redeem(new RedeemCouponCommand("WIOSNA", "alice", "8.8.8.8")));
        verify(couponRepository, never()).recordRedemption(any(), any(), any());
    }

    @Test
    void geoFailurePropagatesAndBlocksRedemption_failClosed() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 0)));
        when(geoLocationProvider.resolveCountry(IP))
                .thenThrow(new GeoLocationUnavailableException("timeout", null));

        assertThatExceptionOfType(GeoLocationUnavailableException.class)
                .isThrownBy(() -> service.redeem(new RedeemCouponCommand("WIOSNA", "alice", "8.8.8.8")));
        verify(couponRepository, never()).recordRedemption(any(), any(), any());
    }

    @Test
    void invalidInputsFailFastBeforeAnyPortCall() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.redeem(new RedeemCouponCommand("WIOSNA", "alice", "not-an-ip")));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.redeem(new RedeemCouponCommand("", "alice", "8.8.8.8")));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.redeem(new RedeemCouponCommand("WIOSNA", " ", "8.8.8.8")));
        verifyNoInteractions(couponRepository, geoLocationProvider);
    }
}
