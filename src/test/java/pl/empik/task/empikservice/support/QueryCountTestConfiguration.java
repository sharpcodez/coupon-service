package pl.empik.task.empikservice.support;

import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class QueryCountTestConfiguration {

    public static final SingleQueryCountHolder QUERY_COUNTS = new SingleQueryCountHolder();

    @Bean
    static BeanPostProcessor queryCountingDataSourceProxy() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof ProxyDataSource)) {
                    return ProxyDataSourceBuilder.create(dataSource)
                            .name("test-ds")
                            .countQuery(QUERY_COUNTS)
                            .build();
                }
                return bean;
            }
        };
    }
}
