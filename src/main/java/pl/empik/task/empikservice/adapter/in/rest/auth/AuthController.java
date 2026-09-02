package pl.empik.task.empikservice.adapter.in.rest.auth;

import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.empik.task.empikservice.adapter.in.rest.auth.dto.TokenRequest;
import pl.empik.task.empikservice.adapter.in.rest.auth.dto.TokenResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;

    AuthController(AuthenticationManager authenticationManager, JwtTokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    TokenResponse token(@Valid @RequestBody TokenRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException(e);
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .toList();
        JwtTokenService.IssuedToken issued = tokenService.issue(authentication.getName(), roles);
        return new TokenResponse(issued.token(), "Bearer", issued.expiresInSeconds());
    }
}
