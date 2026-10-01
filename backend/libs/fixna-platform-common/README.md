# fixna-platform-common

Shared Java infrastructure used by all Fixna API services (`consulting-api`, `localboost-api`, future products).

## Packages

| Package | Contents |
|---------|----------|
| `common.web` | `ApiError`, `FixnaException`, `GlobalExceptionHandler`, security filters, auth entry points |
| `common.config` | CORS, BCrypt password encoder |
| `common.logging` | MDC helpers, sensitive data masking |
| `common.tenant` | `TenantScopeProvider` — each product wires its own tenant context |

## Usage

Add to service `pom.xml`:

```xml
<dependency>
  <groupId>in.fixna</groupId>
  <artifactId>fixna-platform-common</artifactId>
</dependency>
```

Auto-configured via Spring Boot 3 `AutoConfiguration.imports`. Each product must register a `TenantScopeProvider` bridge to its own `TenantContext`.

Build from repo root:

```bash
mvn -f libs/fixna-platform-common/pom.xml install
```
