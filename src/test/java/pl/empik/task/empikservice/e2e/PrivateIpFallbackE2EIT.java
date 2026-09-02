package pl.empik.task.empikservice.e2e;

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
import pl.empik.task.empikservice.support.PostgresTestConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.jwt.secret=e2e-test-secret-0123456789abcdef-0000",
        "app.security.users[0].username=admin",
        "app.security.users[0].password={noop}admin-pass",
        "app.security.users[0].roles[0]=ADMIN",
        "app.security.users[1].username=user",
        "app.security.users[1].password={noop}user-pass",
        "app.security.users[1].roles[0]=USER",
        "app.geoip.private-ip-fallback-country=PL",
        "app.geoip.base-url=http://localhost:1"
})
@Import(PostgresTestConfiguration.class)
@AutoConfigureTestRestTemplate
class PrivateIpFallbackE2EIT {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void reset() {
        jdbcClient.sql("DELETE FROM redemption").update();
        jdbcClient.sql("DELETE FROM coupon").update();
    }

    @Test
    void localhostRedemptionSucceedsThroughTheFallbackCountry() {
        String admin = (String) rest.postForEntity("/api/v1/auth/token",
                Map.of("username", "admin", "password", "admin-pass"), Map.class)
                .getBody().get("accessToken");
        String user = (String) rest.postForEntity("/api/v1/auth/token",
                Map.of("username", "user", "password", "user-pass"), Map.class)
                .getBody().get("accessToken");

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(admin);
        adminHeaders.set("Content-Type", "application/json");
        assertThat(rest.postForEntity("/api/v1/coupons",
                new HttpEntity<>(Map.of("code", "LOCALPL", "maxUsages", 5, "country", "PL"), adminHeaders),
                Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(user);
        ResponseEntity<Map> redeemed = rest.postForEntity("/api/v1/coupons/LOCALPL/redemptions",
                new HttpEntity<>(null, userHeaders), Map.class);
        assertThat(redeemed.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
