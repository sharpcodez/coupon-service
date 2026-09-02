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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.empik.task.empikservice.support.FixedClockTestConfiguration;
import pl.empik.task.empikservice.support.PostgresTestConfiguration;
import pl.empik.task.empikservice.support.QueryCountTestConfiguration;
import pl.empik.task.empikservice.support.TestTokens;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.empik.task.empikservice.support.QueryCountTestConfiguration.QUERY_COUNTS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.jwt.secret=e2e-test-secret-0123456789abcdef-0000",
        "app.security.users[0].username=admin",
        "app.security.users[0].password={noop}admin-pass",
        "app.security.users[0].roles[0]=ADMIN",
        "app.security.users[1].username=user",
        "app.security.users[1].password={noop}user-pass",
        "app.security.users[1].roles[0]=USER",
        "app.http.trusted-proxies=127.0.0.1/32,::1/128",
        "app.geoip.circuit-breaker-minimum-number-of-calls=1000",
        "app.geoip.token=test-token"
})
@Import({PostgresTestConfiguration.class, QueryCountTestConfiguration.class,
        FixedClockTestConfiguration.class})
@AutoConfigureTestRestTemplate
class CouponLifecycleE2EIT {

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
        QUERY_COUNTS.clear();
    }

    private String token(String username, String password) {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/token",
                Map.of("username", username, "password", password), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("accessToken");
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<Map> createCoupon(String token, String code, int maxUsages, String country) {
        HttpHeaders headers = bearer(token);
        headers.set("Content-Type", "application/json");
        return rest.postForEntity("/api/v1/coupons",
                new HttpEntity<>(Map.of("code", code, "maxUsages", maxUsages, "country", country), headers),
                Map.class);
    }

    private ResponseEntity<Map> redeem(String token, String code, String forwardedFor) {
        HttpHeaders headers = bearer(token);
        if (forwardedFor != null) {
            headers.set("X-Forwarded-For", forwardedFor);
        }
        return rest.postForEntity("/api/v1/coupons/" + code + "/redemptions",
                new HttpEntity<>(null, headers), Map.class);
    }

    private void stubCountry(String ip, String country) {
        GEO.stubFor(get(urlPathEqualTo("/lite/" + ip))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ip\":\"" + ip + "\",\"country_code\":\"" + country + "\"}")));
    }

    @Test
    void healthEndpointIsPublic() {
        assertThat(rest.getForEntity("/actuator/health", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void fullLifecycle_createRedeemInspect_caseInsensitive_fixedClockStamps() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        stubCountry("8.8.8.8", "PL");

        ResponseEntity<Map> created = createCoupon(admin, "WIOSNA", 2, "PL");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation().toString()).endsWith("/api/v1/coupons/WIOSNA");
        assertThat(created.getBody().get("createdAt"))
                .isEqualTo(FixedClockTestConfiguration.FIXED_NOW.toString());

        ResponseEntity<Map> redeemed = redeem(user, "wiosna", "8.8.8.8");
        assertThat(redeemed.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(redeemed.getBody().get("couponCode")).isEqualTo("WIOSNA");
        assertThat(redeemed.getBody().get("userId")).isEqualTo("user");
        assertThat(redeemed.getBody().get("redeemedAt"))
                .isEqualTo(FixedClockTestConfiguration.FIXED_NOW.toString());

        HttpHeaders headers = bearer(admin);
        ResponseEntity<Map> fetched = rest.exchange("/api/v1/coupons/WIOSNA",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(fetched.getBody().get("currentUsages")).isEqualTo(1);
    }

    @Test
    void duplicateCreateIsCaseInsensitive_409() {
        String admin = token("admin", "admin-pass");
        assertThat(createCoupon(admin, "WIOSNA", 2, "PL").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> duplicate = createCoupon(admin, "wiosna", 9, "DE");
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_COUPON_CODE");
    }

    @Test
    void sameUserSecondRedemption_409() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        stubCountry("8.8.8.8", "PL");
        createCoupon(admin, "TWICE", 5, "PL");

        assertThat(redeem(user, "TWICE", "8.8.8.8").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<Map> second = redeem(user, "TWICE", "8.8.8.8");
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().get("code")).isEqualTo("COUPON_ALREADY_REDEEMED");
    }

    @Test
    void exhaustedCoupon_409() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        stubCountry("8.8.8.8", "PL");
        createCoupon(admin, "SMALL", 1, "PL");

        assertThat(redeem(user, "SMALL", "8.8.8.8").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<Map> exhausted = redeem(admin, "SMALL", "8.8.8.8");
        assertThat(exhausted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exhausted.getBody().get("code")).isEqualTo("COUPON_EXHAUSTED");
    }

    @Test
    void wrongCountry_403() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        stubCountry("9.9.9.9", "DE");
        createCoupon(admin, "PLONLY", 5, "PL");

        ResponseEntity<Map> rejected = redeem(user, "PLONLY", "9.9.9.9");
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rejected.getBody().get("code")).isEqualTo("COUPON_NOT_VALID_IN_COUNTRY");
    }

    @Test
    void unknownCoupon_404_withoutGeoLookup() {
        String user = token("user", "user-pass");

        ResponseEntity<Map> missing = redeem(user, "GHOST", "8.8.8.8");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody().get("code")).isEqualTo("COUPON_NOT_FOUND");
        GEO.verify(0, getRequestedFor(urlPathMatching("/lite/.*")));
    }

    @Test
    void geoProviderDown_503_failClosed() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        GEO.stubFor(get(urlPathEqualTo("/lite/7.7.7.7"))
                .willReturn(aResponse().withStatus(500)));
        createCoupon(admin, "GEODOWN", 5, "PL");

        ResponseEntity<Map> failed = redeem(user, "GEODOWN", "7.7.7.7");
        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(failed.getBody().get("code")).isEqualTo("GEOLOCATION_UNAVAILABLE");
        assertThat(jdbcClient.sql("SELECT count(*) FROM redemption").query(Long.class).single())
                .isZero();
    }

    @Test
    void noForwardedHeader_loopbackPeerIsPrivate_403_unresolvable() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        createCoupon(admin, "LOCAL", 5, "PL");

        ResponseEntity<Map> rejected = redeem(user, "LOCAL", null);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rejected.getBody().get("code")).isEqualTo("COUNTRY_UNRESOLVABLE");
        GEO.verify(0, getRequestedFor(urlPathMatching("/lite/.*")));
    }

    @Test
    void invalidBody_400_evenWhenAuthorized() {
        String admin = token("admin", "admin-pass");

        ResponseEntity<Map> tooLong = createCoupon(admin, "X".repeat(65), 0, "POL");
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooLong.getBody().get("code")).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void garbageBearerToken_401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-jwt");
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/coupons/X/redemptions",
                new HttpEntity<>(null, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    private static final String E2E_SECRET = "e2e-test-secret-0123456789abcdef-0000";

    @Test
    void wrongSignatureToken_401() {
        String forged = TestTokens.hs256("a-completely-different-secret-32bytes!",
                "user", List.of("USER"), Instant.now().plusSeconds(3600));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(forged);
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/coupons/X/redemptions",
                new HttpEntity<>(null, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void expiredToken_401() {
        String expired = TestTokens.hs256(E2E_SECRET,
                "user", List.of("USER"), Instant.now().minusSeconds(3600));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expired);
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/coupons/X/redemptions",
                new HttpEntity<>(null, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void overlongJwtSubject_400_not500() {
        String admin = token("admin", "admin-pass");
        createCoupon(admin, "LONGSUB", 5, "PL");
        String hostile = TestTokens.hs256(E2E_SECRET,
                "u".repeat(129), List.of("USER"), Instant.now().plusSeconds(3600));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostile);
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/coupons/LONGSUB/redemptions",
                new HttpEntity<>(null, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void createExecutesExactlyOneInsert() {
        String admin = token("admin", "admin-pass");

        QUERY_COUNTS.clear();
        assertThat(createCoupon(admin, "COUNTME", 5, "PL").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var counts = QUERY_COUNTS.getQueryCountMap().get("test-ds");
        assertThat(counts.getInsert()).isEqualTo(1);
        assertThat(counts.getSelect()).isZero();
        assertThat(counts.getUpdate()).isZero();
    }

    @Test
    void redeemExecutesExactlyOneSelectOneInsertOneUpdate() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        stubCountry("8.8.8.8", "PL");
        createCoupon(admin, "LEAN", 5, "PL");

        QUERY_COUNTS.clear();
        assertThat(redeem(user, "LEAN", "8.8.8.8").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var counts = QUERY_COUNTS.getQueryCountMap().get("test-ds");
        assertThat(counts.getSelect()).isEqualTo(1);
        assertThat(counts.getInsert()).isEqualTo(1);
        assertThat(counts.getUpdate()).isEqualTo(1);
        assertThat(counts.getDelete()).isZero();
    }

    @Test
    void proxyAppendedSecondHeaderLineWins_forgedFirstLineIgnored() {
        String admin = token("admin", "admin-pass");
        String user = token("user", "user-pass");
        stubCountry("8.8.8.8", "PL");
        createCoupon(admin, "SPLITHDR", 5, "PL");

        HttpHeaders headers = bearer(user);
        headers.add("X-Forwarded-For", "1.2.3.4");
        headers.add("X-Forwarded-For", "8.8.8.8");
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/coupons/SPLITHDR/redemptions",
                new HttpEntity<>(null, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        GEO.verify(1, getRequestedFor(urlPathEqualTo("/lite/8.8.8.8")));
        GEO.verify(0, getRequestedFor(urlPathEqualTo("/lite/1.2.3.4")));
    }
}
