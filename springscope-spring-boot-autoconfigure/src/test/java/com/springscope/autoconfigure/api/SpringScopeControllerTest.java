package com.springscope.autoconfigure.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springscope.autoconfigure.inspector.BeanInspector;
import com.springscope.autoconfigure.inspector.DependencyResolver;
import com.springscope.autoconfigure.model.BeanGraph;
import com.springscope.autoconfigure.model.BeanMetadata;
import com.springscope.autoconfigure.model.DependencyEdge;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SpringScopeControllerTest {

    @Mock
    private BeanInspector beanInspector;

    @Mock
    private DependencyResolver dependencyResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SpringScopeController(beanInspector, dependencyResolver))
                .addPlaceholderValue("springscope.api.base-path", "/springscope/api")
                .build();
    }

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/springscope/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("0.1.0-SNAPSHOT"));
    }

    @Test
    void getBeanReturns404WhenMissing() throws Exception {
        when(beanInspector.findByName("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/springscope/api/beans/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Bean not found: missing"));
    }

    @Test
    void graphReturnsNodesAndEdges() throws Exception {
        when(dependencyResolver.buildGraph(any())).thenReturn(new BeanGraph(
                List.of(new BeanGraph.Node("paymentService", "paymentService")),
                List.of(DependencyEdge.dependency("paymentController", "paymentService"))
        ));

        mockMvc.perform(get("/springscope/api/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].id").value("paymentService"))
                .andExpect(jsonPath("$.edges[0].type").value("DEPENDENCY"));
    }

    @Test
    void getBeanReturnsMetadata() throws Exception {
        when(beanInspector.findByName("paymentService")).thenReturn(Optional.of(
                new BeanMetadata("paymentService", "demo.PaymentService", "singleton", true, false, false, false,
                        "APPLICATION", null, null, false)
        ));

        mockMvc.perform(get("/springscope/api/beans/paymentService"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("paymentService"))
                .andExpect(jsonPath("$.singleton").value(true));
    }
}
