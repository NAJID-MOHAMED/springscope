package com.springscope.springscope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springscope.springscope.support.LazyGreetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpringScopeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/springscope/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("0.1.0-SNAPSHOT"));
    }

    @Test
    void listsDemoBeansIncludingScopesAndLazyMetadata() throws Exception {
        mockMvc.perform(get("/springscope/api/beans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='paymentController')]").exists())
                .andExpect(jsonPath("$[?(@.name=='paymentService')]").exists())
                .andExpect(jsonPath("$[?(@.name=='paymentRepository')]").exists())
                .andExpect(jsonPath("$[?(@.name=='auditLogger' && @.singleton==true)]").exists())
                .andExpect(jsonPath("$[?(@.name=='prototypeWorker' && @.prototype==true)]").exists())
                .andExpect(jsonPath("$[?(@.name=='lazyGreetingService' && @.lazyInit==true)]").exists());

        assertThat(LazyGreetingService.INSTANTIATED).isFalse();
    }

    @Test
    void returnsPaymentServiceDetails() throws Exception {
        mockMvc.perform(get("/springscope/api/beans/paymentService"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("paymentService"))
                .andExpect(jsonPath("$.type").value("com.springscope.springscope.payment.PaymentService"))
                .andExpect(jsonPath("$.scope").value("singleton"))
                .andExpect(jsonPath("$.singleton").value(true));
    }

    @Test
    void returnsUnknownBeanAsNotFound() throws Exception {
        mockMvc.perform(get("/springscope/api/beans/doesNotExist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void graphContainsPaymentDependencyChain() throws Exception {
        mockMvc.perform(get("/springscope/api/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[?(@.id=='paymentController')]").exists())
                .andExpect(jsonPath("$.nodes[?(@.id=='paymentService')]").exists())
                .andExpect(jsonPath("$.nodes[?(@.id=='paymentRepository')]").exists())
                .andExpect(jsonPath("$.edges[?(@.source=='paymentController' && @.target=='paymentService' && @.type=='DEPENDENCY')]").exists())
                .andExpect(jsonPath("$.edges[?(@.source=='paymentService' && @.target=='paymentRepository' && @.type=='DEPENDENCY')]").exists());
    }

    @Test
    void filtersBeansByType() throws Exception {
        mockMvc.perform(get("/springscope/api/beans").param("type", "PaymentService"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("paymentService"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
