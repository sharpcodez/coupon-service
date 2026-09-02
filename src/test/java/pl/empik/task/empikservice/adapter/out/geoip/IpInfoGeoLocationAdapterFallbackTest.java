package pl.empik.task.empikservice.adapter.out.geoip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.IpAddress;

import static org.assertj.core.api.Assertions.assertThat;

@RestClientTest(IpInfoGeoLocationAdapter.class)
@EnableConfigurationProperties(GeoIpProperties.class)
@TestPropertySource(properties = {
        "app.geoip.base-url=http://geoip.test",
        "app.geoip.token=test-token",
        "app.geoip.cache-ttl=1h",
        "app.geoip.cache-max-size=100",
        "app.geoip.private-ip-fallback-country=PL",
        "app.geoip.circuit-breaker-failure-rate-threshold=50",
        "app.geoip.circuit-breaker-sliding-window-size=10",
        "app.geoip.circuit-breaker-minimum-number-of-calls=5",
        "app.geoip.circuit-breaker-wait-duration-in-open-state=30s"
})
class IpInfoGeoLocationAdapterFallbackTest {

    @Autowired
    private IpInfoGeoLocationAdapter adapter;
    @Autowired
    private MockRestServiceServer server;

    @Test
    void privateIpUsesConfiguredFallbackCountry_noHttpCall() {
        assertThat(adapter.resolveCountry(IpAddress.of("127.0.0.1"))).isEqualTo(Country.of("PL"));
        assertThat(adapter.resolveCountry(IpAddress.of("192.168.1.10"))).isEqualTo(Country.of("PL"));
        server.verify();
    }
}
