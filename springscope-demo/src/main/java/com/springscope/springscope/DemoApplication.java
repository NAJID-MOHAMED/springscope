package com.springscope.springscope;

import com.springscope.springscope.support.AuditLogger;
import com.springscope.springscope.support.LazyGreetingService;
import com.springscope.springscope.support.PrototypeWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Configuration
public class DemoApplication {

    @Bean
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }

    @Bean
    @Lazy
    public LazyGreetingService lazyGreetingService() {
        return new LazyGreetingService();
    }

    @Bean
    @Scope("prototype")
    public PrototypeWorker prototypeWorker() {
        return new PrototypeWorker();
    }
}
