package com.springscope.autoconfigure.inspector;

import com.springscope.autoconfigure.model.BeanFilter;
import com.springscope.autoconfigure.model.BeanMetadata;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

public class BeanInspector {

    private static final Logger log = LoggerFactory.getLogger(BeanInspector.class);

    private final ConfigurableListableBeanFactory beanFactory;

    public BeanInspector(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public List<BeanMetadata> listBeans() {
        return listBeans(BeanFilter.unconstrained());
    }

    public List<BeanMetadata> listBeans(BeanFilter filter) {
        BeanFilter effectiveFilter = filter == null ? BeanFilter.unconstrained() : filter;
        List<BeanMetadata> beans = new ArrayList<>();
        for (String beanName : discoverBeanNames()) {
            inspectSafely(beanName).ifPresent(beans::add);
        }
        beans.sort(Comparator.comparing(BeanMetadata::name, String.CASE_INSENSITIVE_ORDER));
        if (effectiveFilter.isUnconstrained()) {
            return List.copyOf(beans);
        }
        return beans.stream().filter(effectiveFilter::matches).toList();
    }

    public Optional<BeanMetadata> findByName(String beanName) {
        if (beanName == null || beanName.isBlank()) {
            return Optional.empty();
        }
        String canonical = canonicalName(beanName);
        if (!containsBean(canonical) && !containsBean(beanName)) {
            return Optional.empty();
        }
        return inspectSafely(containsBean(canonical) ? canonical : beanName);
    }

    private Optional<BeanMetadata> inspectSafely(String beanName) {
        try {
            return Optional.of(inspect(beanName));
        }
        catch (Exception ex) {
            log.debug("Skipping bean '{}' because metadata could not be read: {}", beanName, ex.toString());
            return Optional.empty();
        }
    }

    private BeanMetadata inspect(String beanName) {
        BeanDefinition definition = getBeanDefinition(beanName);
        Class<?> type = resolveType(beanName, definition);
        String scope = resolveScope(definition);
        boolean singleton = isSingleton(beanName, definition, scope);
        boolean prototype = isPrototype(definition, scope);
        boolean lazyInit = definition != null && definition.isLazyInit();
        return new BeanMetadata(
                beanName,
                typeName(type, definition),
                scope,
                singleton,
                prototype,
                detectAopProxy(beanName, type),
                lazyInit,
                resolveRole(definition),
                definition != null ? emptyToNull(definition.getFactoryBeanName()) : null,
                definition != null ? emptyToNull(definition.getFactoryMethodName()) : null,
                isFactoryBean(beanName, definition)
        );
    }

    private Set<String> discoverBeanNames() {
        Set<String> names = new LinkedHashSet<>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            try {
                BeanDefinition definition = getBeanDefinition(name);
                if (definition != null && definition.isAbstract()) {
                    continue;
                }
                names.add(name);
            }
            catch (Exception ex) {
                log.debug("Skipping bean '{}' while discovering names: {}", name, ex.toString());
                names.add(name);
            }
        }
        for (String name : beanFactory.getSingletonNames()) {
            if (!beanFactory.containsBeanDefinition(name)) {
                names.add(name);
            }
        }
        return names;
    }

    @Nullable
    private BeanDefinition getBeanDefinition(String beanName) {
        String canonical = canonicalName(beanName);
        if (beanFactory.containsBeanDefinition(canonical)) {
            return beanFactory.getBeanDefinition(canonical);
        }
        if (beanFactory.containsBeanDefinition(beanName)) {
            return beanFactory.getBeanDefinition(beanName);
        }
        return null;
    }

    @Nullable
    private Class<?> resolveType(String beanName, @Nullable BeanDefinition definition) {
        try {
            Class<?> type = beanFactory.getType(beanName, false);
            if (type != null) {
                return type;
            }
        }
        catch (Exception ex) {
            log.debug("Could not resolve type for bean '{}': {}", beanName, ex.toString());
        }
        if (definition instanceof AbstractBeanDefinition abstractDefinition && abstractDefinition.hasBeanClass()) {
            return abstractDefinition.getBeanClass();
        }
        if (definition != null) {
            ResolvableType resolvableType = definition.getResolvableType();
            if (resolvableType != ResolvableType.NONE) {
                return resolvableType.resolve();
            }
        }
        return null;
    }

    private String resolveScope(@Nullable BeanDefinition definition) {
        if (definition == null) {
            return ConfigurableBeanFactory.SCOPE_SINGLETON;
        }
        String scope = definition.getScope();
        if (scope == null || scope.isBlank()) {
            return ConfigurableBeanFactory.SCOPE_SINGLETON;
        }
        return scope;
    }

    private boolean isSingleton(String beanName, @Nullable BeanDefinition definition, String scope) {
        if (definition != null) {
            return definition.isSingleton();
        }
        return beanFactory.containsSingleton(beanName) || ConfigurableBeanFactory.SCOPE_SINGLETON.equals(scope);
    }

    private boolean isPrototype(@Nullable BeanDefinition definition, String scope) {
        if (definition != null) {
            return definition.isPrototype();
        }
        return ConfigurableBeanFactory.SCOPE_PROTOTYPE.equals(scope);
    }

    @Nullable
    private Boolean detectAopProxy(String beanName, @Nullable Class<?> type) {
        if (beanFactory.containsSingleton(beanName)) {
            Object instance = beanFactory.getSingleton(beanName);
            if (instance != null) {
                return AopUtils.isAopProxy(instance);
            }
        }
        if (type == null) {
            return null;
        }
        if (SpringProxy.class.isAssignableFrom(type) || Advised.class.isAssignableFrom(type)) {
            return true;
        }
        String typeName = type.getName();
        if (typeName.contains("$$SpringCGLIB$$")
                || typeName.contains("$$EnhancerBySpringCGLIB$$")
                || typeName.contains("$Proxy")) {
            return true;
        }
        return false;
    }

    @Nullable
    private Boolean isFactoryBean(String beanName, @Nullable BeanDefinition definition) {
        try {
            return beanFactory.isFactoryBean(beanName);
        }
        catch (Exception ex) {
            if (definition instanceof AbstractBeanDefinition abstractDefinition && abstractDefinition.hasBeanClass()) {
                return FactoryBean.class.isAssignableFrom(abstractDefinition.getBeanClass());
            }
            return null;
        }
    }

    private String resolveRole(@Nullable BeanDefinition definition) {
        if (definition == null) {
            return "APPLICATION";
        }
        return switch (definition.getRole()) {
            case BeanDefinition.ROLE_INFRASTRUCTURE -> "INFRASTRUCTURE";
            case BeanDefinition.ROLE_SUPPORT -> "SUPPORT";
            default -> "APPLICATION";
        };
    }

    @Nullable
    private String typeName(@Nullable Class<?> type, @Nullable BeanDefinition definition) {
        if (type != null) {
            return type.getName();
        }
        if (definition != null) {
            return emptyToNull(definition.getBeanClassName());
        }
        return null;
    }

    private boolean containsBean(String beanName) {
        return beanFactory.containsBean(beanName);
    }

    private String canonicalName(String beanName) {
        return BeanFactoryUtils.transformedBeanName(beanName);
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
