package pl.empik.task.empikservice.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.http")
public record HttpProperties(List<String> trustedProxies) {

    public HttpProperties {
        trustedProxies = trustedProxies == null
                ? List.of()
                : trustedProxies.stream().filter(cidr -> !cidr.isBlank()).toList();
    }
}
