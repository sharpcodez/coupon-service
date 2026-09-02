package pl.empik.task.empikservice.domain.port.in;

import pl.empik.task.empikservice.domain.model.Coupon;

public interface GetCouponUseCase {
    Coupon get(String code);
}
