package pl.empik.task.empikservice.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.empik.task.empikservice.support.PostgresTestConfiguration;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.jwt.secret=e2e-test-secret-0123456789abcdef-0000",
        "app.security.users[0].username=admin",
        "app.security.users[0].password={noop}admin-pass",
        "app.security.users[0].roles[0]=ADMIN",
        "app.security.users[1].username=user",
        "app.security.users[1].password={noop}user-pass",
        "app.security.users[1].roles[0]=USER",
        "app.http.trusted-proxies=",
        "app.geoip.token=test-token"
})
@Import(PostgresTestConfiguration.class)
@AutoConfigureTestRestTemplate
class XffSpoofingE2EIT {

    static final WireMockServer GEO = new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void startGeo() {
        GEO.start();
    }

    @AfterAll
    static void stopGeo() {
        GEO.stop();
    }

    @DynamicPropertySource
    static void geoUrl(DynamicPropertyRegistry registry) {
        registry.add("app.geoip.base-url", GEO::baseUrl);
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void reset() {
        jdbcClient.sql("DELETE FROM redemption").update();
        jdbcClient.sql("DELETE FROM coupon").update();
        GEO.resetAll();
    }

    @Test
    void forgedXForwardedForCannotBuyACountry() {
        String admin = tokenFor("admin", "admin-pass");
        String user = tokenFor("user", "user-pass");
        create(admin, "SPOOF", "PL");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user);
        headers.set("X-Forwarded-For", "8.8.8.8");
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/coupons/SPOOF/redemptions",
                new HttpEntity<>(null, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("COUNTRY_UNRESOLVABLE");
        GEO.verify(0, getRequestedFor(urlPathMatching("/lite/.*")));
    }

    private String tokenFor(String username, String password) {
        return (String) rest.postForEntity("/api/v1/auth/token",
                Map.of("username", username, "password", password), Map.class)
                .getBody().get("accessToken");
    }

    private void create(String token, String code, String country) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Content-Type", "application/json");
        assertThat(rest.postForEntity("/api/v1/coupons",
                new HttpEntity<>(Map.of("code", code, "maxUsages", 5, "country", country), headers),
                Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
