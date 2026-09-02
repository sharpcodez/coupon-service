package pl.empik.task.empikservice.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.security")
public record UsersProperties(List<UserSpec> users) {

    public record UserSpec(String username, String password, List<String> roles) {}

    public UsersProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }
}
