package com.springscope.autoconfigure.inspector;

import com.springscope.autoconfigure.model.BeanFilter;
import com.springscope.autoconfigure.model.BeanGraph;
import com.springscope.autoconfigure.model.BeanMetadata;
import com.springscope.autoconfigure.model.DependencyEdge;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class DependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);

    private final ConfigurableListableBeanFactory beanFactory;
    private final BeanInspector beanInspector;

    public DependencyResolver(ConfigurableListableBeanFactory beanFactory, BeanInspector beanInspector) {
        this.beanFactory = beanFactory;
        this.beanInspector = beanInspector;
    }

    public BeanGraph buildGraph() {
        return buildGraph(BeanFilter.unconstrained());
    }

    public BeanGraph buildGraph(BeanFilter filter) {
        BeanFilter effectiveFilter = filter == null ? BeanFilter.unconstrained() : filter;
        List<BeanMetadata> allBeans = beanInspector.listBeans();
        Set<String> knownBeans = new LinkedHashSet<>();
        for (BeanMetadata bean : allBeans) {
            knownBeans.add(bean.name());
        }

        Set<String> included = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (effectiveFilter.isUnconstrained()) {
            included.addAll(knownBeans);
        }
        else {
            for (BeanMetadata bean : allBeans) {
                if (effectiveFilter.matches(bean)) {
                    included.add(bean.name());
                }
            }
            // Include the direct dependencies of matching beans. This is a second
            // pass so bean names stay canonical and filtering remains deterministic.
            for (String beanName : List.copyOf(included)) {
                for (String dependency : dependenciesOf(beanName)) {
                    String canonical = canonicalName(dependency);
                    if (knownBeans.contains(canonical)) {
                        included.add(canonical);
                    }
                }
            }
        }

        List<BeanGraph.Node> nodes = included.stream()
                .map(name -> new BeanGraph.Node(name, name))
                .toList();

        Set<DependencyEdge> uniqueEdges = new LinkedHashSet<>();
        for (String source : included) {
            for (String dependency : dependenciesOf(source)) {
                String target = canonicalName(dependency);
                if (source.equals(target) || !included.contains(target)) {
                    continue;
                }
                uniqueEdges.add(DependencyEdge.dependency(source, target));
            }
        }

        List<DependencyEdge> edges = uniqueEdges.stream()
                .sorted(Comparator.comparing(DependencyEdge::source, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DependencyEdge::target, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DependencyEdge::type))
                .toList();

        return new BeanGraph(nodes, edges);
    }

    private List<String> dependenciesOf(String beanName) {
        Set<String> dependencies = new LinkedHashSet<>();
        addAll(dependencies, safeDependenciesForBean(beanName));
        return List.copyOf(dependencies);
    }

    private String[] safeDependenciesForBean(String beanName) {
        try {
            return beanFactory.getDependenciesForBean(beanName);
        }
        catch (Exception ex) {
            log.debug("No dependency metadata for bean '{}': {}", beanName, ex.toString());
            return new String[0];
        }
    }

    private String canonicalName(String beanName) {
        return BeanFactoryUtils.transformedBeanName(beanName);
    }

    private static void addAll(Set<String> target, String[] values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value);
            }
        }
    }
}
