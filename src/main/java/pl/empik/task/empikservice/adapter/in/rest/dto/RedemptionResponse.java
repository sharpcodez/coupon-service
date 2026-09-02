package pl.empik.task.empikservice.adapter.in.rest.dto;

import pl.empik.task.empikservice.domain.model.Redemption;

import java.time.Instant;

public record RedemptionResponse(String couponCode, String userId, Instant redeemedAt) {

    public static RedemptionResponse from(Redemption redemption) {
        return new RedemptionResponse(redemption.couponCode().value(),
                redemption.userId().value(), redemption.redeemedAt());
    }
}
