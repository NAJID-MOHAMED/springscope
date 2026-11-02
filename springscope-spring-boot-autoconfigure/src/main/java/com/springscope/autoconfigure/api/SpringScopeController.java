package com.springscope.autoconfigure.api;

import com.springscope.autoconfigure.SpringScopeVersion;
import com.springscope.autoconfigure.exception.SpringScopeException;
import com.springscope.autoconfigure.inspector.BeanInspector;
import com.springscope.autoconfigure.inspector.DependencyResolver;
import com.springscope.autoconfigure.model.BeanFilter;
import com.springscope.autoconfigure.model.BeanGraph;
import com.springscope.autoconfigure.model.BeanMetadata;
import com.springscope.autoconfigure.model.HealthResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Development-oriented inspection API. These endpoints expose application structure
 * and are intended for local use. Protect or disable them in shared environments.
 */
@ResponseBody
@RestController
@RequestMapping("${springscope.api.base-path:/springscope/api}")
public class SpringScopeController {

    private final BeanInspector beanInspector;
    private final DependencyResolver dependencyResolver;

    public SpringScopeController(BeanInspector beanInspector, DependencyResolver dependencyResolver) {
        this.beanInspector = beanInspector;
        this.dependencyResolver = dependencyResolver;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.up(SpringScopeVersion.CURRENT);
    }

    /**
     * Lists bean metadata. Query parameters are combined with AND:
     * {@code name} is a case-insensitive substring of the bean name,
     * {@code type} matches a fully-qualified or simple class name,
     * {@code scope} is a case-insensitive exact scope name,
     * {@code lazy} filters on lazy-init.
     */
    @GetMapping("/beans")
    public List<BeanMetadata> listBeans(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Boolean lazy) {
        return beanInspector.listBeans(new BeanFilter(name, type, scope, lazy));
    }

    @GetMapping("/beans/{beanName}")
    public BeanMetadata getBean(@PathVariable String beanName) {
        return beanInspector.findByName(beanName)
                .orElseThrow(() -> SpringScopeException.beanNotFound(beanName));
    }

    /**
     * Returns known Spring container dependencies. When {@code name} or {@code type}
     * is provided, matching beans and their direct dependencies are included.
     */
    @GetMapping("/graph")
    public BeanGraph graph(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type) {
        return dependencyResolver.buildGraph(new BeanFilter(name, type, null, null));
    }

    @ExceptionHandler(SpringScopeException.class)
    public ResponseEntity<Map<String, Object>> handleSpringScopeException(SpringScopeException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getStatusCode() == 404 ? "Not Found" : "SpringScope Error");
        body.put("message", exception.getMessage());
        body.put("status", exception.getStatusCode());
        return ResponseEntity.status(exception.getStatusCode()).body(body);
    }
}
