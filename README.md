# SpringScope

SpringScope is an open-source Spring Boot developer tool for inspecting what a running application is doing inside the Spring container.

This repository currently ships a reusable **Spring Boot Starter**. Add the starter to an existing Spring Boot application and SpringScope registers itself through auto-configuration. A React dashboard and runtime tracing are planned; they are not part of this MVP.

**These endpoints expose application internals. Use them locally, keep them off public networks, and disable SpringScope in production unless you explicitly protect the API.**

## Implemented now (MVP)

- Spring Boot auto-configuration with an on/off switch
- Bean metadata inspection without instantiating lazy beans
- Dependency graph built from Spring container dependency metadata
- REST API for health, bean listing, bean details, and the graph
- A demo application with a realistic constructor-injection chain

## Planned (not implemented)

- Runtime method-flow tracing
- React dashboard
- Spring Security integration
- Richer bean details (annotations, injected fields, profiles)
- Complete discovery of every Java-level dependency or method call

## Architecture

SpringScope follows the standard Spring Boot starter layout:

1. An application adds `springscope-spring-boot-starter`.
2. Spring Boot loads `SpringScopeAutoConfiguration` from `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
3. The auto-configuration registers `BeanInspector`, `DependencyResolver`, and (in servlet web apps) `SpringScopeController`.
4. Inspection reads `BeanDefinition` metadata and `ConfigurableListableBeanFactory` APIs. It does not call `getBean()` to discover types.

```text
Application
    └── springscope-spring-boot-starter
            └── springscope-spring-boot-autoconfigure
                    ├── BeanInspector
                    ├── DependencyResolver
                    └── SpringScopeController  (servlet applications only)
```

## Maven modules

| Module | Responsibility |
|---|---|
| `springscope` | Parent/aggregator POM. Coordinates the reactor. Not an executable application. |
| `springscope-spring-boot-autoconfigure` | Implementation: properties, inspection, graph, REST API, auto-configuration. |
| `springscope-spring-boot-starter` | Dependency-only starter for consuming applications. |
| `springscope-demo` | Sample Spring Boot app used to develop and verify SpringScope. |

## Installation

Requires Java 21 and Spring Boot 3.2.x.

Install this repository into the local Maven repository:

```powershell
.\mvnw.cmd clean install
```

Then add the starter to a Spring Boot application:

```xml
<dependency>
    <groupId>com.springscope</groupId>
    <artifactId>springscope-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

No manual `@Import` or component scanning of SpringScope is required.

## Configuration

```yaml
springscope:
  enabled: true
  api:
    base-path: /springscope/api
```

| Property | Default | Description |
|---|---|---|
| `springscope.enabled` | `true` | Registers inspection services and the REST API. Set `false` to disable SpringScope entirely. |
| `springscope.api.base-path` | `/springscope/api` | Base path for the REST API. Must start with `/`. |

### Security

SpringScope does not expose bean property values, credentials, or environment variables. It still reveals class names, bean names, scopes, and known dependencies.

Recommendations:

- Leave it enabled only on developer machines and local demos.
- Set `springscope.enabled=false` in production profiles.
- Do not publish the API on the public internet.
- If you later enable it behind Spring Security, restrict `/springscope/api/**` to local or authenticated developers. First-class Spring Security integration is a future configuration concern, not part of this MVP.

## REST API

Default base path: `/springscope/api`.

### Health

```http
GET /springscope/api/health
```

```json
{
  "status": "UP",
  "version": "0.1.0-SNAPSHOT"
}
```

### List beans

```http
GET /springscope/api/beans
GET /springscope/api/beans?name=payment
GET /springscope/api/beans?type=PaymentService
GET /springscope/api/beans?scope=prototype
GET /springscope/api/beans?lazy=true
```

Query parameters are combined with AND:

- `name` — case-insensitive substring of the bean name
- `type` — fully-qualified or simple class name
- `scope` — case-insensitive exact scope (`singleton`, `prototype`, ...)
- `lazy` — `true` or `false`

Each item looks like:

```json
{
  "name": "paymentService",
  "type": "com.springscope.springscope.payment.PaymentService",
  "scope": "singleton",
  "singleton": true,
  "prototype": false,
  "aopProxy": false,
  "lazyInit": false,
  "role": "APPLICATION",
  "factoryBeanName": null,
  "factoryMethodName": null,
  "factoryBean": false
}
```

`aopProxy` is populated when it can be detected without creating the bean (already-cached singletons or proxy-looking types). Otherwise it may be `null`.

### Bean details

```http
GET /springscope/api/beans/{beanName}
```

Returns `404` when the bean does not exist.

### Dependency graph

```http
GET /springscope/api/graph
GET /springscope/api/graph?name=paymentController
GET /springscope/api/graph?type=PaymentService
```

Optional `name` and `type` filters include matching beans **and their direct dependencies**. Edges always point from a bean to a bean it depends on.

```json
{
  "nodes": [
    { "id": "paymentController", "name": "paymentController" },
    { "id": "paymentService", "name": "paymentService" },
    { "id": "paymentRepository", "name": "paymentRepository" }
  ],
  "edges": [
    { "source": "paymentController", "target": "paymentService", "type": "DEPENDENCY" },
    { "source": "paymentService", "target": "paymentRepository", "type": "DEPENDENCY" }
  ]
}
```

## Run the demo

```powershell
.\mvnw.cmd -pl springscope-demo spring-boot:run
```

Then:

```text
http://localhost:8080/springscope/api/health
http://localhost:8080/springscope/api/beans
http://localhost:8080/springscope/api/beans/paymentService
http://localhost:8080/springscope/api/graph
http://localhost:8080/payments/status
```

The demo wires this constructor-injection chain:

```text
PaymentController → PaymentService → PaymentRepository
```

It also registers a singleton (`auditLogger`), a prototype (`prototypeWorker`), and a lazy bean (`lazyGreetingService`).

## Known limitations

- Graph data comes from Spring's `getDependenciesForBean()`. That is **not** a complete map of every field, method, or programmatic lookup in the application.
- Beans that have not been created yet (especially lazy beans) may have little or no dependency metadata.
- Unresolvable types are reported from bean-definition class names when possible; some beans have no resolvable class.
- Inspection is limited to the `ApplicationContext` that loaded the starter, not every parent or child context.
- AOP proxy detection is best-effort and never instantiates a bean just to check it.
- The API is unauthenticated.

## Roadmap

1. **Bean Explorer UI** — React dashboard over the current REST API.
2. **Richer metadata** — annotations, profiles, and injected dependency names.
3. **Runtime flow tracing** — method execution, duration, and call relationships.
4. **Security** — documented Spring Security samples for locking down the API.

## License

See [LICENSE](LICENSE).
