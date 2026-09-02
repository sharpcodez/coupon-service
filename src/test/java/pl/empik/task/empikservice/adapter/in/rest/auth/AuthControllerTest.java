package pl.empik.task.empikservice.adapter.in.rest.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pl.empik.task.empikservice.adapter.in.rest.ApiExceptionHandler;
import pl.empik.task.empikservice.adapter.in.rest.ProblemJsonAccessDeniedHandler;
import pl.empik.task.empikservice.adapter.in.rest.ProblemJsonAuthenticationEntryPoint;
import pl.empik.task.empikservice.infrastructure.ClockConfig;
import pl.empik.task.empikservice.infrastructure.PropertiesConfig;
import pl.empik.task.empikservice.infrastructure.SecurityConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, PropertiesConfig.class, ClockConfig.class, ApiExceptionHandler.class,
        ProblemJsonAuthenticationEntryPoint.class, ProblemJsonAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=unit-test-secret-0123456789abcdef!!",
        "app.security.jwt.issuer=test-issuer",
        "app.security.jwt.ttl=1h",
        "app.security.users[0].username=admin",
        "app.security.users[0].password={noop}admin-pass",
        "app.security.users[0].roles[0]=ADMIN"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void issuesDecodableTokenWithSubjectRolesIssuerAndTtl() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","password":"admin-pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");
        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("test-issuer");
    }

    @Test
    void wrongPasswordYields401ProblemJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void blankCredentialsYield400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType("application/json")
                        .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void protectedUrlWithoutTokenGets401ProblemJsonFromEntryPoint() throws Exception {
        mockMvc.perform(post("/api/v1/coupons").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void unknownUrlYields401NotEndpointDisclosure() throws Exception {
        mockMvc.perform(post("/api/v1/nonexistent"))
                .andExpect(status().isUnauthorized());
    }
}
