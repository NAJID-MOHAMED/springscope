package com.springscope.autoconfigure.inspector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.springscope.autoconfigure.model.BeanFilter;
import com.springscope.autoconfigure.model.BeanMetadata;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.BeanDefinition;

@ExtendWith(MockitoExtension.class)
class BeanInspectorTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void extractsSingletonPrototypeAndLazyMetadataWithoutInstantiatingLazyBeans() {
        LazyService.INSTANTIATED = false;
        context = new AnnotationConfigApplicationContext(SampleBeans.class);
        BeanInspector inspector = new BeanInspector(context.getBeanFactory());

        BeanMetadata singleton = inspector.findByName("singletonService").orElseThrow();
        assertThat(singleton.singleton()).isTrue();
        assertThat(singleton.prototype()).isFalse();
        assertThat(singleton.scope()).isEqualTo("singleton");
        assertThat(singleton.type()).isEqualTo(SingletonService.class.getName());
        assertThat(singleton.lazyInit()).isFalse();
        assertThat(singleton.aopProxy()).isFalse();
        assertThat(singleton.role()).isEqualTo("APPLICATION");
        assertThat(singleton.factoryMethodName()).isEqualTo("singletonService");

        BeanMetadata prototype = inspector.findByName("prototypeService").orElseThrow();
        assertThat(prototype.prototype()).isTrue();
        assertThat(prototype.singleton()).isFalse();
        assertThat(prototype.scope()).isEqualTo("prototype");

        BeanMetadata lazy = inspector.findByName("lazyService").orElseThrow();
        assertThat(lazy.lazyInit()).isTrue();
        assertThat(lazy.singleton()).isTrue();
        assertThat(LazyService.INSTANTIATED).isFalse();

        List<BeanMetadata> lazyOnly = inspector.listBeans(new BeanFilter(null, null, null, true));
        assertThat(lazyOnly).extracting(BeanMetadata::name).containsExactly("lazyService");
    }

    @Test
    void includesUnknownTypesFromBeanClassName() {
        context = new AnnotationConfigApplicationContext();
        RootBeanDefinition definition = new RootBeanDefinition();
        definition.setBeanClassName("com.springscope.doesnotexist.MissingType");
        context.registerBeanDefinition("unknownTypeBean", definition);
        BeanInspector inspector = new BeanInspector(context.getBeanFactory());
        BeanMetadata metadata = inspector.findByName("unknownTypeBean").orElseThrow();
        assertThat(metadata.type()).isEqualTo("com.springscope.doesnotexist.MissingType");
    }

    @Test
    void inspectsEmptyBeanDefinitions() {
        context = new AnnotationConfigApplicationContext();
        context.registerBeanDefinition("emptyDefinition", new RootBeanDefinition());
        BeanInspector inspector = new BeanInspector(context.getBeanFactory());
        BeanMetadata metadata = inspector.findByName("emptyDefinition").orElseThrow();
        assertThat(metadata.name()).isEqualTo("emptyDefinition");
        assertThat(metadata.scope()).isEqualTo("singleton");
        assertThat(metadata.singleton()).isTrue();
    }

    @Test
    void isolatesErrorsWhenASingleBeanCannotBeResolved(@Mock ConfigurableListableBeanFactory beanFactory) {
        RootBeanDefinition goodDefinition = new RootBeanDefinition(String.class);
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[] {"good", "broken"});
        when(beanFactory.getSingletonNames()).thenReturn(new String[0]);
        when(beanFactory.containsBeanDefinition("good")).thenReturn(true);
        when(beanFactory.containsBeanDefinition("broken")).thenReturn(true);
        when(beanFactory.getBeanDefinition("good")).thenReturn(goodDefinition);
        when(beanFactory.getBeanDefinition("broken")).thenThrow(new IllegalStateException("unreadable definition"));
        org.mockito.Mockito.doReturn(String.class).when(beanFactory).getType("good", false);
        when(beanFactory.containsSingleton("good")).thenReturn(false);
        when(beanFactory.isFactoryBean("good")).thenReturn(false);

        BeanInspector inspector = new BeanInspector(beanFactory);
        List<BeanMetadata> beans = inspector.listBeans();

        assertThat(beans).extracting(BeanMetadata::name).containsExactly("good");
    }

    @Test
    void reportsInfrastructureRole() {
        context = new AnnotationConfigApplicationContext(SampleBeans.class);
        BeanInspector inspector = new BeanInspector(context.getBeanFactory());
        BeanMetadata infrastructure = inspector.findByName("infrastructureMarker").orElseThrow();
        assertThat(infrastructure.role()).isEqualTo("INFRASTRUCTURE");
    }

    @Configuration
    static class SampleBeans {

        @Bean
        SingletonService singletonService() {
            return new SingletonService();
        }

        @Bean
        @Scope("prototype")
        PrototypeService prototypeService() {
            return new PrototypeService();
        }

        @Bean
        @Lazy
        LazyService lazyService() {
            return new LazyService();
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        InfrastructureMarker infrastructureMarker() {
            return new InfrastructureMarker();
        }
    }

    static class SingletonService {
    }

    static class PrototypeService {
    }

    static class LazyService {
        static boolean INSTANTIATED;

        LazyService() {
            INSTANTIATED = true;
        }
    }

    static class InfrastructureMarker {
    }
}
