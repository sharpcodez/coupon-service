package pl.empik.task.empikservice.adapter.out.geoip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.empik.task.empikservice.domain.exception.CountryUnresolvableException;
import pl.empik.task.empikservice.domain.exception.GeoLocationUnavailableException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.IpAddress;
import pl.empik.task.empikservice.domain.port.out.GeoLocationProvider;

@Component
class IpInfoGeoLocationAdapter implements GeoLocationProvider {

    private static final Logger log = LoggerFactory.getLogger(IpInfoGeoLocationAdapter.class);

    private final RestClient restClient;
    private final Cache<String, Country> cache;
    private final CircuitBreaker circuitBreaker;
    private final Country privateIpFallback;
    private final String authorizationHeader;

    IpInfoGeoLocationAdapter(RestClient.Builder restClientBuilder, GeoIpProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.cacheTtl())
                .maximumSize(properties.cacheMaxSize())
                .build();
        this.circuitBreaker = CircuitBreaker.of("geoip", CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.circuitBreakerFailureRateThreshold())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(properties.circuitBreakerSlidingWindowSize())
                .minimumNumberOfCalls(properties.circuitBreakerMinimumNumberOfCalls())
                .waitDurationInOpenState(properties.circuitBreakerWaitDurationInOpenState())
                .build());
        this.privateIpFallback = properties.hasPrivateIpFallback()
                ? Country.of(properties.privateIpFallbackCountry())
                : null;
        if (properties.hasToken()) {
            this.authorizationHeader = "Bearer " + properties.token();
        } else {
            this.authorizationHeader = null;
            log.warn("no GeoIP token configured (GEOIP_TOKEN) — public-IP lookups will fail closed");
        }
    }

    @Override
    public Country resolveCountry(IpAddress ip) {
        if (!ip.isPublic()) {
            if (privateIpFallback == null) {
                throw new CountryUnresolvableException(ip);
            }
            return privateIpFallback;
        }
        Country cached = cache.getIfPresent(ip.literal());
        if (cached != null) {
            return cached;
        }
        Country resolved = lookup(ip);
        cache.put(ip.literal(), resolved);
        return resolved;
    }

    private Country lookup(IpAddress ip) {
        IpInfoLiteResponse response;
        try {
            response = circuitBreaker.executeSupplier(() -> requestFor(ip)
                    .retrieve()
                    .body(IpInfoLiteResponse.class));
        } catch (CallNotPermittedException e) {
            throw new GeoLocationUnavailableException("geolocation provider circuit is open", e);
        } catch (RuntimeException e) {
            throw new GeoLocationUnavailableException("geolocation lookup failed", e);
        }
        if (response == null || Boolean.TRUE.equals(response.bogon())
                || response.countryCode() == null || response.countryCode().isBlank()) {
            throw new CountryUnresolvableException(ip);
        }
        try {
            return Country.of(response.countryCode());
        } catch (IllegalArgumentException e) {
            throw new CountryUnresolvableException(ip);
        }
    }

    private RestClient.RequestHeadersSpec<?> requestFor(IpAddress ip) {
        RestClient.RequestHeadersUriSpec<?> request = restClient.get();
        if (authorizationHeader == null) {
            return request.uri("/lite/{ip}", ip.literal());
        }
        return request.uri("/lite/{ip}", ip.literal()).header(HttpHeaders.AUTHORIZATION, authorizationHeader);
    }

    record IpInfoLiteResponse(
            String ip,
            @JsonProperty("country_code") String countryCode,
            Boolean bogon) {}
}
