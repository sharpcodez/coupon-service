package pl.empik.task.empikservice.adapter.out.geoip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import pl.empik.task.empikservice.domain.exception.CountryUnresolvableException;
import pl.empik.task.empikservice.domain.exception.GeoLocationUnavailableException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.IpAddress;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestClientTest(IpInfoGeoLocationAdapter.class)
@EnableConfigurationProperties(GeoIpProperties.class)
@TestPropertySource(properties = {
        "app.geoip.base-url=http://geoip.test",
        "app.geoip.token=test-token",
        "app.geoip.cache-ttl=1h",
        "app.geoip.cache-max-size=100",
        "app.geoip.private-ip-fallback-country=",
        "app.geoip.circuit-breaker-failure-rate-threshold=50",
        "app.geoip.circuit-breaker-sliding-window-size=10",
        "app.geoip.circuit-breaker-minimum-number-of-calls=100",
        "app.geoip.circuit-breaker-wait-duration-in-open-state=30s"
})
class IpInfoGeoLocationAdapterTest {

    @Autowired
    private IpInfoGeoLocationAdapter adapter;
    @Autowired
    private MockRestServiceServer server;

    private static String url(String ip) {
        return "http://geoip.test/lite/" + ip;
    }

    @Test
    void resolvesCountryForPublicIp() {
        server.expect(requestTo(url("8.8.8.8")))
                .andRespond(withSuccess("""
                        {"ip":"8.8.8.8","country_code":"US"}
                        """, APPLICATION_JSON));

        assertThat(adapter.resolveCountry(IpAddress.of("8.8.8.8"))).isEqualTo(Country.of("US"));
        server.verify();
    }

    @Test
    void cachesSuccessfulLookups_oneHttpCallForTwoResolves() {
        server.expect(once(), requestTo(url("1.1.1.1")))
                .andRespond(withSuccess("""
                        {"ip":"1.1.1.1","country_code":"PL"}
                        """, APPLICATION_JSON));

        assertThat(adapter.resolveCountry(IpAddress.of("1.1.1.1"))).isEqualTo(Country.of("PL"));
        assertThat(adapter.resolveCountry(IpAddress.of("1.1.1.1"))).isEqualTo(Country.of("PL"));
        server.verify();
    }

    @Test
    void bogonResponseMeansCountryUnresolvable() {
        server.expect(requestTo(url("100.64.0.7")))
                .andRespond(withSuccess("""
                        {"ip":"100.64.0.7","bogon":true}
                        """, APPLICATION_JSON));

        assertThatExceptionOfType(CountryUnresolvableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("100.64.0.7")));
    }

    @Test
    void missingCountryCodeMeansCountryUnresolvable() {
        server.expect(requestTo(url("8.8.4.4")))
                .andRespond(withSuccess("""
                        {"ip":"8.8.4.4"}
                        """, APPLICATION_JSON));

        assertThatExceptionOfType(CountryUnresolvableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("8.8.4.4")));
    }

    @Test
    void tokenIsAttachedToProviderRequests() {
        server.expect(requestTo(url("6.6.6.6")))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("""
                        {"ip":"6.6.6.6","country_code":"US"}
                        """, APPLICATION_JSON));

        assertThat(adapter.resolveCountry(IpAddress.of("6.6.6.6"))).isEqualTo(Country.of("US"));
        server.verify();
    }

    @Test
    void unauthorizedTokenFailsClosed() {
        server.expect(requestTo(url("5.5.5.5"))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatExceptionOfType(GeoLocationUnavailableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("5.5.5.5")));
    }

    @Test
    void httpErrorFailsClosed() {
        server.expect(requestTo(url("9.9.9.9"))).andRespond(withServerError());

        assertThatExceptionOfType(GeoLocationUnavailableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("9.9.9.9")));
    }

    @Test
    void ioTimeoutFailsClosed() {
        server.expect(requestTo(url("9.9.9.8")))
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatExceptionOfType(GeoLocationUnavailableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("9.9.9.8")));
    }

    @Test
    void privateIpIsRejectedLocallyWithoutFallback_noHttpCall() {
        assertThatExceptionOfType(CountryUnresolvableException.class)
                .isThrownBy(() -> adapter.resolveCountry(IpAddress.of("10.0.0.1")));
        server.verify();
    }
}
