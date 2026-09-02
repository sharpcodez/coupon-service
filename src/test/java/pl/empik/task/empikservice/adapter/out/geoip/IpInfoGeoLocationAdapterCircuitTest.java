package pl.empik.task.empikservice.adapter.out.geoip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import pl.empik.task.empikservice.domain.exception.GeoLocationUnavailableException;
import pl.empik.task.empikservice.domain.model.IpAddress;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@RestClientTest(IpInfoGeoLocationAdapter.class)
@EnableConfigurationProperties(GeoIpProperties.class)
@TestPropertySource(properties = {
        "app.geoip.base-url=http://geoip.test",
        "app.geoip.token=test-token",
        "app.geoip.cache-ttl=1h",
        "app.geoip.cache-max-size=100",
        "app.geoip.private-ip-fallback-country=",
        "app.geoip.circuit-breaker-failure-rate-threshold=50",
        "app.geoip.circuit-breaker-sliding-window-size=2",
        "app.geoip.circuit-breaker-minimum-number-of-calls=2",
        "app.geoip.circuit-breaker-wait-duration-in-open-state=30s"
})
class IpInfoGeoLocationAdapterCircuitTest {

    @Autowired
    private IpInfoGeoLocationAdapter adapter;
    @Autowired
    private MockRestServiceServer server;

    private static String url(String ip) {
        return "http://geoip.test/lite/" + ip;
    }

    @Test
    void circuitOpensAfterRepeatedFailures_noFurtherProviderCalls() {
        server.expect(requestTo(url("2.2.2.2"))).andRespond(withServerError());
        server.expect(requestTo(url("3.3.3.3"))).andRespond(withServerError());

        assertThatExceptionOfType(GeoLocationUnavailableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("2.2.2.2")));
        assertThatExceptionOfType(GeoLocationUnavailableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("3.3.3.3")));
        assertThatExceptionOfType(GeoLocationUnavailableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("4.4.4.4")));
        server.verify();
    }
}
