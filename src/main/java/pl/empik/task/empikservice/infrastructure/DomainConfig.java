package pl.empik.task.empikservice.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.GetCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase;
import pl.empik.task.empikservice.domain.port.out.CouponRepository;
import pl.empik.task.empikservice.domain.port.out.GeoLocationProvider;
import pl.empik.task.empikservice.domain.service.CreateCouponService;
import pl.empik.task.empikservice.domain.service.GetCouponService;
import pl.empik.task.empikservice.domain.service.RedeemCouponService;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class DomainConfig {

    @Bean
    CreateCouponUseCase createCouponUseCase(CouponRepository couponRepository, Clock clock) {
        return new CreateCouponService(couponRepository, clock);
    }

    @Bean
    GetCouponUseCase getCouponUseCase(CouponRepository couponRepository) {
        return new GetCouponService(couponRepository);
    }

    @Bean
    RedeemCouponUseCase redeemCouponUseCase(CouponRepository couponRepository,
                                            GeoLocationProvider geoLocationProvider,
                                            Clock clock) {
        return new RedeemCouponService(couponRepository, geoLocationProvider, clock);
    }
}
