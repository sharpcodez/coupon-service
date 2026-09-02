package pl.empik.task.empikservice.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pl.empik.task.empikservice.domain.exception.CountryUnresolvableException;
import pl.empik.task.empikservice.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.task.empikservice.domain.exception.CouponExhaustedException;
import pl.empik.task.empikservice.domain.exception.CouponNotFoundException;
import pl.empik.task.empikservice.domain.exception.CouponNotValidInCountryException;
import pl.empik.task.empikservice.domain.exception.DuplicateCouponCodeException;
import pl.empik.task.empikservice.domain.exception.GeoLocationUnavailableException;
import pl.empik.task.empikservice.domain.model.Country;
import pl.empik.task.empikservice.domain.model.Coupon;
import pl.empik.task.empikservice.domain.model.CouponCode;
import pl.empik.task.empikservice.domain.model.IpAddress;
import pl.empik.task.empikservice.domain.model.Redemption;
import pl.empik.task.empikservice.domain.model.UserId;
import pl.empik.task.empikservice.domain.port.in.CreateCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.GetCouponUseCase;
import pl.empik.task.empikservice.domain.port.in.RedeemCouponUseCase;
import pl.empik.task.empikservice.infrastructure.ClockConfig;
import pl.empik.task.empikservice.infrastructure.PropertiesConfig;
import pl.empik.task.empikservice.infrastructure.SecurityConfig;
import pl.empik.task.empikservice.infrastructure.WebConfig;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CouponController.class, RedemptionController.class})
@Import({SecurityConfig.class, PropertiesConfig.class, ClockConfig.class, WebConfig.class,
        ApiExceptionHandler.class,
        ProblemJsonAuthenticationEntryPoint.class, ProblemJsonAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=unit-test-secret-0123456789abcdef!!",
        "app.security.jwt.issuer=test-issuer",
        "app.security.jwt.ttl=1h",
        "app.http.trusted-proxies="
})
class CouponApiTest {

    private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateCouponUseCase createCoupon;
    @MockitoBean
    private GetCouponUseCase getCoupon;
    @MockitoBean
    private RedeemCouponUseCase redeemCoupon;

    private static RequestPostProcessor admin() {
        return jwt().jwt(jwt -> jwt.subject("admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor user(String subject) {
        return jwt().jwt(jwt -> jwt.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static Coupon wiosna() {
        return Coupon.reconstitute(CouponCode.of("WIOSNA"), NOW, 5, 0, Country.of("PL"));
    }

    @Test
    void adminCreatesCoupon_201_locationAndBody() throws Exception {
        when(createCoupon.create(any())).thenReturn(wiosna());

        mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("application/json")
                        .content("""
                                {"code":"wiosna","maxUsages":5,"country":"pl"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/coupons/WIOSNA"))
                .andExpect(jsonPath("$.code").value("WIOSNA"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-26T12:00:00Z"))
                .andExpect(jsonPath("$.maxUsages").value(5))
                .andExpect(jsonPath("$.currentUsages").value(0))
                .andExpect(jsonPath("$.country").value("PL"));
    }

    @Test
    void userRoleCannotCreate_403() throws Exception {
        mockMvc.perform(post("/api/v1/coupons").with(user("alice"))
                        .contentType("application/json")
                        .content("""
                                {"code":"X","maxUsages":1,"country":"PL"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void noTokenCannotCreate_401() throws Exception {
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType("application/json")
                        .content("""
                                {"code":"X","maxUsages":1,"country":"PL"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void beanValidationCaps_400_notFiveHundred() throws Exception {
        String tooLongCode = "X".repeat(65);
        mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("application/json")
                        .content("""
                                {"code":"%s","maxUsages":0,"country":"POL"}
                                """.formatted(tooLongCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void domainLevelValidationMapsTo400() throws Exception {
        when(createCoupon.create(any()))
                .thenThrow(new IllegalArgumentException("not an ISO 3166-1 alpha-2 country code: ZZ"));

        mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("application/json")
                        .content("""
                                {"code":"X","maxUsages":1,"country":"ZZ"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void duplicateCode_409_withCodeNotInternalId() throws Exception {
        when(createCoupon.create(any()))
                .thenThrow(new DuplicateCouponCodeException(CouponCode.of("WIOSNA"), null));

        mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("application/json")
                        .content("""
                                {"code":"WIOSNA","maxUsages":5,"country":"PL"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_COUPON_CODE"))
                .andExpect(jsonPath("$.detail").value(containsString("WIOSNA")));
    }

    @Test
    void malformedJson_400() throws Exception {
        mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("application/json")
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unexpectedFailure_500_genericDetailNothingLeaked() throws Exception {
        when(createCoupon.create(any())).thenThrow(new IllegalStateException("hikari pool exploded"));

        String body = mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("application/json")
                        .content("""
                                {"code":"X","maxUsages":1,"country":"PL"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("hikari");
    }

    @Test
    void wrongContentType_415_not500() throws Exception {
        mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("text/plain")
                        .content("code=WIOSNA"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void dataIntegrityFallback_409() throws Exception {
        when(createCoupon.create(any()))
                .thenThrow(new DataIntegrityViolationException("value too long"));

        mockMvc.perform(post("/api/v1/coupons").with(admin())
                        .contentType("application/json")
                        .content("""
                                {"code":"X","maxUsages":1,"country":"PL"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    @Test
    void adminReadsCoupon_200() throws Exception {
        when(getCoupon.get("wiosna")).thenReturn(wiosna());

        mockMvc.perform(get("/api/v1/coupons/wiosna").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WIOSNA"));
    }

    @Test
    void unknownCoupon_404() throws Exception {
        when(getCoupon.get("nope")).thenThrow(new CouponNotFoundException(CouponCode.of("NOPE")));

        mockMvc.perform(get("/api/v1/coupons/nope").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"));
    }

    @Test
    void redeemTakesUserIdFromJwtSubjectAndIpFromPeer_ignoringForgedHeader() throws Exception {
        when(redeemCoupon.redeem(any())).thenReturn(
                new Redemption(CouponCode.of("WIOSNA"), UserId.of("alice"), NOW));

        mockMvc.perform(post("/api/v1/coupons/wiosna/redemptions").with(user("alice"))
                        .header("X-Forwarded-For", "8.8.8.8")
                        .header("X-Forwarded-For", "1.2.3.4")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.9");
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.couponCode").value("WIOSNA"))
                .andExpect(jsonPath("$.userId").value("alice"))
                .andExpect(jsonPath("$.redeemedAt").value("2026-08-26T12:00:00Z"));

        ArgumentCaptor<RedeemCouponUseCase.RedeemCouponCommand> captor =
                ArgumentCaptor.forClass(RedeemCouponUseCase.RedeemCouponCommand.class);
        verify(redeemCoupon).redeem(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("wiosna");
        assertThat(captor.getValue().userId()).isEqualTo("alice");
        assertThat(captor.getValue().clientIp()).isEqualTo("203.0.113.9");
    }

    @Test
    void redeemWithoutToken_401() throws Exception {
        mockMvc.perform(post("/api/v1/coupons/wiosna/redemptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void domainOutcomesMapToDistinctResponses() throws Exception {
        record Case(RuntimeException exception, int status, String code) {}
        List<Case> cases = List.of(
                new Case(new CouponNotFoundException(CouponCode.of("WIOSNA")), 404, "COUPON_NOT_FOUND"),
                new Case(new CouponNotValidInCountryException(CouponCode.of("WIOSNA"), Country.of("DE")),
                        403, "COUPON_NOT_VALID_IN_COUNTRY"),
                new Case(new CountryUnresolvableException(IpAddress.of("127.0.0.1")),
                        403, "COUNTRY_UNRESOLVABLE"),
                new Case(new CouponExhaustedException(CouponCode.of("WIOSNA")), 409, "COUPON_EXHAUSTED"),
                new Case(new CouponAlreadyRedeemedException(CouponCode.of("WIOSNA"), UserId.of("alice"), null),
                        409, "COUPON_ALREADY_REDEEMED"),
                new Case(new GeoLocationUnavailableException("down", null), 503, "GEOLOCATION_UNAVAILABLE"));

        for (Case testCase : cases) {
            reset(redeemCoupon);
            when(redeemCoupon.redeem(any())).thenThrow(testCase.exception());
            mockMvc.perform(post("/api/v1/coupons/wiosna/redemptions").with(user("alice")))
                    .andExpect(status().is(testCase.status()))
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value(testCase.code()));
        }
    }
}
