package com.amit.ems.common.config;

import com.amit.ems.common.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
public class CorrelationIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(
            name = "correlationIdFilterRegistration"
    )
    public FilterRegistrationBean<CorrelationIdFilter>
    correlationIdFilterRegistration() {

        CorrelationIdFilter filter =
                new CorrelationIdFilter();

        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setName("correlationIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");

        return registration;
    }
}