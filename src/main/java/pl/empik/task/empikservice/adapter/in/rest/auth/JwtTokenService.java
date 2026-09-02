package pl.empik.task.empikservice.adapter.in.rest.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, String issuer, Duration ttl, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttl = ttl;
        this.clock = clock;
    }

    public IssuedToken issue(String username, Collection<String> roles) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("roles", List.copyOf(roles))
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new IssuedToken(token, ttl.toSeconds());
    }

    public record IssuedToken(String token, long expiresInSeconds) {}
}
