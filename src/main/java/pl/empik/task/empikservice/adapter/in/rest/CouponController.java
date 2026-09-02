package pl.empik.task.empikservice.adapter.in.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.empik.task.empikservice.adapter.in.rest.dto.CouponResponse;
import pl.empik.task.empikservice.adapter.in.rest.dto.CreateCouponRequest;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase.CreateCouponCommand;
import pl.empik.task.empikservice.domain.port.in.GetCouponUseCase;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/coupons")
class CouponController {

    private final CreateCouponUseCase createCoupon;
    private final GetCouponUseCase getCoupon;

    CouponController(CreateCouponUseCase createCoupon, GetCouponUseCase getCoupon) {
        this.createCoupon = createCoupon;
        this.getCoupon = getCoupon;
    }

    @PostMapping
    ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = createCoupon.create(new CreateCouponCommand(
                request.code(), request.maxUsages(), request.country()));
        return ResponseEntity
                .created(URI.create("/api/v1/coupons/" + coupon.code().value()))
                .body(CouponResponse.from(coupon));
    }

    @GetMapping("/{code}")
    CouponResponse get(@PathVariable String code) {
        return CouponResponse.from(getCoupon.get(code));
    }
}
