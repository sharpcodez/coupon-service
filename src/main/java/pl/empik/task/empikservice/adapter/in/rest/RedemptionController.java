package pl.empik.task.empikservice.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.empik.task.empikservice.adapter.in.rest.dto.RedemptionResponse;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase.RedeemCouponCommand;

import java.util.Collections;

@RestController
@RequestMapping("/api/v1/coupons/{code}/redemptions")
class RedemptionController {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final RedeemCouponUseCase redeemCoupon;
    private final ClientIpResolver clientIpResolver;

    RedemptionController(RedeemCouponUseCase redeemCoupon, ClientIpResolver clientIpResolver) {
        this.redeemCoupon = redeemCoupon;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RedemptionResponse redeem(@PathVariable String code,
                              JwtAuthenticationToken authentication,
                              HttpServletRequest request) {
        String clientIp = clientIpResolver.resolve(
                request.getRemoteAddr(), allForwardedForHeaderLines(request));
        Redemption redemption = redeemCoupon.redeem(new RedeemCouponCommand(
                code, authentication.getToken().getSubject(), clientIp));
        return RedemptionResponse.from(redemption);
    }

    private static String allForwardedForHeaderLines(HttpServletRequest request) {
        String joined = String.join(",", Collections.list(request.getHeaders(X_FORWARDED_FOR)));
        return joined.isEmpty() ? null : joined;
    }
}
