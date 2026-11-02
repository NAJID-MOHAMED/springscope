package com.springscope.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.springscope.autoconfigure.api.SpringScopeController;
import com.springscope.autoconfigure.inspector.BeanInspector;
import com.springscope.autoconfigure.inspector.DependencyResolver;
import com.springscope.autoconfigure.properties.SpringScopeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SpringScopeAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringScopeAutoConfiguration.class));

    private final ApplicationContextRunner nonWebRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringScopeAutoConfiguration.class));

    @Test
    void autoConfigurationIsActiveByDefaultInServletApplications() {
        webRunner.run(context -> {
            assertThat(context).hasSingleBean(SpringScopeProperties.class);
            assertThat(context).hasSingleBean(BeanInspector.class);
            assertThat(context).hasSingleBean(DependencyResolver.class);
            assertThat(context).hasSingleBean(SpringScopeController.class);
            assertThat(context.getBean(SpringScopeProperties.class).isEnabled()).isTrue();
            assertThat(context.getBean(SpringScopeProperties.class).getApi().getBasePath()).isEqualTo("/springscope/api");
        });
    }

    @Test
    void canBeDisabledWithProperty() {
        webRunner.withPropertyValues("springscope.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(BeanInspector.class);
            assertThat(context).doesNotHaveBean(DependencyResolver.class);
            assertThat(context).doesNotHaveBean(SpringScopeController.class);
        });
    }

    @Test
    void usesDefaultConfigurationWhenPropertiesAreMissing() {
        webRunner.run(context -> {
            SpringScopeProperties properties = context.getBean(SpringScopeProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getApi().getBasePath()).isEqualTo("/springscope/api");
        });
    }

    @Test
    void doesNotOverrideCustomBeans() {
        webRunner.withUserConfiguration(CustomInspectorConfiguration.class).run(context -> {
            assertThat(context.getBean(BeanInspector.class)).isInstanceOf(CustomBeanInspector.class);
            assertThat(context.getBeansOfType(BeanInspector.class)).hasSize(1);
        });
    }

    @Test
    void doesNotRegisterControllerInNonServletContexts() {
        nonWebRunner.run(context -> {
            assertThat(context).hasSingleBean(BeanInspector.class);
            assertThat(context).hasSingleBean(DependencyResolver.class);
            assertThat(context).doesNotHaveBean(SpringScopeController.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomInspectorConfiguration {

        @Bean
        BeanInspector beanInspector(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
            return new CustomBeanInspector(beanFactory);
        }
    }

    static class CustomBeanInspector extends BeanInspector {
        CustomBeanInspector(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
            super(beanFactory);
        }
    }
}
