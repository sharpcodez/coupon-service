package pl.empik.task.empikservice.domain.service;

import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.model.IpAddress;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.model.UserId;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;
import pl.empik.task.empikservice.domain.port.out.GeoLocationProvider;

import java.time.Clock;

public class RedeemCouponService implements RedeemCouponUseCase {

    private final CouponRepository couponRepository;
    private final GeoLocationProvider geoLocationProvider;
    private final Clock clock;

    public RedeemCouponService(CouponRepository couponRepository,
                               GeoLocationProvider geoLocationProvider,
                               Clock clock) {
        this.couponRepository = couponRepository;
        this.geoLocationProvider = geoLocationProvider;
        this.clock = clock;
    }

    @Override
    public Redemption redeem(RedeemCouponCommand command) {
        CouponCode code = CouponCode.of(command.code());
        UserId userId = UserId.of(command.userId());
        IpAddress clientIp = IpAddress.of(command.clientIp());

        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponNotFoundException(code));
        Country requestCountry = geoLocationProvider.resolveCountry(clientIp);
        coupon.ensureRedeemableFrom(requestCountry);

        return couponRepository.recordRedemption(code, userId, clock.instant());
    }
}
