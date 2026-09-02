package pl.empik.task.empikservice.domain.port.in;

import pl.empik.task.empikservice.domain.model.Coupon;

public interface CreateCouponUseCase {
    Coupon create(CreateCouponCommand command);
    record CreateCouponCommand(String code, int maxUsages, String country) {}
}
