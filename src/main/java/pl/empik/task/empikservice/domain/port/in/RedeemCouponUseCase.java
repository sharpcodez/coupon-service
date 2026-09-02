package pl.empik.task.empikservice.domain.port.in;

import pl.empik.task.empikservice.domain.model.Redemption;

public interface RedeemCouponUseCase {
    Redemption redeem(RedeemCouponCommand command);
    record RedeemCouponCommand(String code, String userId, String clientIp) {}
}
