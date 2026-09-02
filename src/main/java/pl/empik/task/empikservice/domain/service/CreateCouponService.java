package pl.empik.task.empikservice.domain.service;

import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;

import java.time.Clock;

public class CreateCouponService implements CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final Clock clock;

    public CreateCouponService(CouponRepository couponRepository, Clock clock) {
        this.couponRepository = couponRepository;
        this.clock = clock;
    }

    @Override
    public Coupon create(CreateCouponCommand command) {
        Coupon coupon = Coupon.create(
                CouponCode.of(command.code()),
                command.maxUsages(),
                Country.of(command.country()),
                clock.instant());
        return couponRepository.save(coupon);
    }
}
