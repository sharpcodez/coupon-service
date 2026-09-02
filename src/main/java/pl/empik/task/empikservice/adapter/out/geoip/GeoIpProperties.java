package pl.empik.task.empikservice.adapter.out.geoip;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.geoip")
public record GeoIpProperties(
        String baseUrl,
        String token,
        Duration cacheTtl,
        long cacheMaxSize,
        String privateIpFallbackCountry,
        float circuitBreakerFailureRateThreshold,
        int circuitBreakerSlidingWindowSize,
        int circuitBreakerMinimumNumberOfCalls,
        Duration circuitBreakerWaitDurationInOpenState) {

    public boolean hasPrivateIpFallback() {
        return privateIpFallbackCountry != null && !privateIpFallbackCountry.isBlank();
    }

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }
}
