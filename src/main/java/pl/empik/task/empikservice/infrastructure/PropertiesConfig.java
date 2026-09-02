package pl.empik.task.empikservice.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import pl.empik.task.empikservice.adapter.out.geoip.GeoIpProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, UsersProperties.class,
        GeoIpProperties.class, HttpProperties.class})
public class PropertiesConfig {
}
