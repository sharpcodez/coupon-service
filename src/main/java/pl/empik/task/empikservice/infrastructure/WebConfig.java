package pl.empik.task.empikservice.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.empik.task.empikservice.adapter.in.rest.ClientIpResolver;

@Configuration(proxyBeanMethods = false)
public class WebConfig {

    @Bean
    ClientIpResolver clientIpResolver(HttpProperties httpProperties) {
        return new ClientIpResolver(httpProperties.trustedProxies());
    }
}
