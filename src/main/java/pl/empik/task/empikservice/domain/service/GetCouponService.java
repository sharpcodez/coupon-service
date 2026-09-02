package pl.empik.task.empikservice.domain.service;

import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.port.in.GetCouponUseCase;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;

public class GetCouponService implements GetCouponUseCase {

    private final CouponRepository couponRepository;

    public GetCouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public Coupon get(String code) {
        CouponCode couponCode = CouponCode.of(code);
        return couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new CouponNotFoundException(couponCode));
    }
}
