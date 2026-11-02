package com.springscope.autoconfigure;

import com.springscope.autoconfigure.api.SpringScopeController;
import com.springscope.autoconfigure.inspector.BeanInspector;
import com.springscope.autoconfigure.inspector.DependencyResolver;
import com.springscope.autoconfigure.properties.SpringScopeProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@EnableConfigurationProperties(SpringScopeProperties.class)
@ConditionalOnProperty(prefix = "springscope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringScopeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BeanInspector beanInspector(ConfigurableListableBeanFactory beanFactory) {
        return new BeanInspector(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public DependencyResolver dependencyResolver(
            ConfigurableListableBeanFactory beanFactory,
            BeanInspector beanInspector) {
        return new DependencyResolver(beanFactory, beanInspector);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class SpringScopeWebConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SpringScopeController springScopeController(
                BeanInspector beanInspector,
                DependencyResolver dependencyResolver) {
            return new SpringScopeController(beanInspector, dependencyResolver);
        }
    }
}
