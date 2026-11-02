package com.springscope.autoconfigure.inspector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.springscope.autoconfigure.model.BeanFilter;
import com.springscope.autoconfigure.model.BeanGraph;
import com.springscope.autoconfigure.model.BeanMetadata;
import com.springscope.autoconfigure.model.DependencyEdge;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ExtendWith(MockitoExtension.class)
class DependencyResolverTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void buildsDirectedEdgesFromConstructorInjection() {
        context = new AnnotationConfigApplicationContext(PaymentConfig.class);
        BeanInspector inspector = new BeanInspector(context.getBeanFactory());
        DependencyResolver resolver = new DependencyResolver(context.getBeanFactory(), inspector);

        BeanGraph graph = resolver.buildGraph();

        assertThat(graph.nodes()).extracting(BeanGraph.Node::id)
                .contains("paymentController", "paymentService", "paymentRepository");
        assertThat(graph.edges()).contains(
                DependencyEdge.dependency("paymentController", "paymentService"),
                DependencyEdge.dependency("paymentService", "paymentRepository")
        );
        assertThat(graph.edges()).doesNotContain(
                DependencyEdge.dependency("paymentService", "paymentController")
        );
    }

    @Test
    void producesDeterministicOrdering() {
        context = new AnnotationConfigApplicationContext(PaymentConfig.class);
        BeanInspector inspector = new BeanInspector(context.getBeanFactory());
        DependencyResolver resolver = new DependencyResolver(context.getBeanFactory(), inspector);

        BeanGraph first = resolver.buildGraph();
        BeanGraph second = resolver.buildGraph();

        assertThat(first.nodes()).containsExactlyElementsOf(second.nodes());
        assertThat(first.edges()).containsExactlyElementsOf(second.edges());
        assertThat(first.nodes()).isSortedAccordingTo((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.id(), right.id()));
    }

    @Test
    void removesDuplicatesSelfReferencesAndUnknownTargets(
            @Mock ConfigurableListableBeanFactory beanFactory,
            @Mock BeanInspector inspector) {
        BeanMetadata controller = metadata("paymentController");
        BeanMetadata service = metadata("paymentService");
        when(inspector.listBeans()).thenReturn(List.of(controller, service));
        when(beanFactory.getDependenciesForBean("paymentController"))
                .thenReturn(new String[] {"paymentService", "paymentService", "paymentController", "missingRepository"});
        when(beanFactory.getDependenciesForBean("paymentService")).thenReturn(new String[0]);

        DependencyResolver resolver = new DependencyResolver(beanFactory, inspector);
        BeanGraph graph = resolver.buildGraph();

        assertThat(graph.nodes()).extracting(BeanGraph.Node::id)
                .containsExactly("paymentController", "paymentService");
        assertThat(graph.edges()).containsExactly(DependencyEdge.dependency("paymentController", "paymentService"));
        assertThat(graph.edges()).noneMatch(edge -> edge.source().equals(edge.target()));
        assertThat(graph.edges()).noneMatch(edge -> "missingRepository".equals(edge.target()));
    }

    @Test
    void treatsMissingDependencyMetadataAsEmpty(@Mock ConfigurableListableBeanFactory beanFactory, @Mock BeanInspector inspector) {
        when(inspector.listBeans()).thenReturn(List.of(metadata("orphan")));
        when(beanFactory.getDependenciesForBean("orphan")).thenThrow(new IllegalStateException("no metadata"));

        DependencyResolver resolver = new DependencyResolver(beanFactory, inspector);
        BeanGraph graph = resolver.buildGraph();

        assertThat(graph.nodes()).extracting(BeanGraph.Node::id).containsExactly("orphan");
        assertThat(graph.edges()).isEmpty();
    }

    @Test
    void filteredGraphOnlyContainsMatchingBeansAndDirectDependencies() {
        context = new AnnotationConfigApplicationContext(PaymentConfig.class);
        BeanInspector inspector = new BeanInspector(context.getBeanFactory());
        DependencyResolver resolver = new DependencyResolver(context.getBeanFactory(), inspector);

        BeanGraph graph = resolver.buildGraph(new BeanFilter(null, "PaymentController", null, null));

        assertThat(graph.nodes()).extracting(BeanGraph.Node::id)
                .contains("paymentController", "paymentService")
                .doesNotContain("paymentRepository");
        assertThat(graph.edges()).contains(DependencyEdge.dependency("paymentController", "paymentService"));
        assertThat(graph.edges()).allMatch(edge ->
                graph.nodes().stream().anyMatch(node -> node.id().equals(edge.source()))
                        && graph.nodes().stream().anyMatch(node -> node.id().equals(edge.target())));
    }

    private static BeanMetadata metadata(String name) {
        return new BeanMetadata(name, name, "singleton", true, false, false, false, "APPLICATION", null, null, false);
    }

    @Configuration
    static class PaymentConfig {

        @Bean
        PaymentRepository paymentRepository() {
            return new PaymentRepository();
        }

        @Bean
        PaymentService paymentService(PaymentRepository paymentRepository) {
            return new PaymentService(paymentRepository);
        }

        @Bean
        PaymentController paymentController(PaymentService paymentService) {
            return new PaymentController(paymentService);
        }
    }

    static class PaymentRepository {
    }

    static class PaymentService {
        PaymentService(PaymentRepository paymentRepository) {
        }
    }

    static class PaymentController {
        PaymentController(PaymentService paymentService) {
        }
    }
}
