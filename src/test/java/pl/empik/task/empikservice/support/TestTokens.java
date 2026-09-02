package pl.empik.task.empikservice.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class TestTokens {

    private TestTokens() {
    }

    public static String hs256(String secret, String subject, List<String> roles, Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("coupon-service")
                    .subject(subject)
                    .claim("roles", roles)
                    .issueTime(Date.from(expiresAt.minusSeconds(3600)))
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("cannot forge test token", e);
        }
    }
}
