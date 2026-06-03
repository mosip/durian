# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Project Overview

**Durian** is the **MOSIP Datashare Service** — a Spring Boot microservice within the MOSIP (Modular Open Source Identity Platform) ID lifecycle. It securely stores binary data (biometric packets, UIN PDFs) in object storage and returns time-limited, policy-governed URLs that trusted partners use to retrieve that data.

Callers from Registration Processor (ABIS Handler, Manual Adjudication, Verification Stage, Print Service) POST data to Durian; ABIS systems, adjudicators, and print services then GET the data back via the returned URL.

## Build Commands

All commands run from the repo root (the Maven parent at `pom.xml`):

```bash
# Full build — skip Javadoc and GPG for local dev
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true

# Run all tests
mvn test

# Run a single test class
mvn test -pl data-share/data-share-service -Dtest=ClassName

# Run a single test method
mvn test -pl data-share/data-share-service -Dtest=ClassName#methodName

# Build only the service module
mvn clean install -pl data-share/data-share-service -am -Dmaven.javadoc.skip=true -Dgpg.skip=true

# Coverage report (output: data-share/data-share-service/target/site/jacoco/index.html)
mvn clean verify

# SonarQube static analysis
mvn clean verify -Psonar

# Generate OpenAPI JSON (starts/stops the service during integration-test phase)
mvn verify -Popenapi-doc-generate-profile -pl data-share/data-share-service
```

## Running the Service

The service requires a running **Spring Cloud Config Server** on `http://localhost:51000/config` before it will start. Configuration files (`application-default.properties`, `data-share-default.properties`) must exist in the [mosip-config](https://github.com/mosip/mosip-config) repository.

```bash
java \
  -Dspring.cloud.config.uri=http://localhost:51000/config \
  -Dspring.cloud.config.label=master \
  -Dspring.profiles.active=default \
  -jar data-share/data-share-service/target/data-share-service-<version>.jar
```

The service listens on port `8097` at servlet path `/v1/datashare` (both configurable). Swagger UI is at `/v1/datashare/swagger-ui.html`.

**Standalone mode** (no external service dependencies) is enabled by adding `standalone` to `spring.profiles.active` and setting the properties below.

## Architecture

### Request Flow

1. **POST `/v1/datashare/create/{policyId}/{subscriberId}`** — caller uploads a file (`multipart/form-data`).
2. `DataShareController` delegates to `DataShareServiceImpl`.
3. In normal mode, `PolicyUtil` fetches the sharing policy from **Partner Management Service** (cached in Spring's `partnerpolicyCache`, evicted on a configurable schedule).
4. Based on `encryptionType` in the policy:
   - `"Partner Based"` → `EncryptionUtil` calls **Cryptomanager** to encrypt the bytes.
   - `"none"` → data is stored as-is.
5. Simultaneously (via `CompletableFuture` on a dedicated `dataShareTaskExecutor` thread pool), `DigitalSignatureUtil` calls **Keymanager** for a JWT signature over the file's SHA-256 digest.
6. `DataShareServiceImpl` stores the encrypted bytes in **Object Store** (S3-compatible via `khazana` `S3Adapter`) with metadata: policyId, subscriberId, expiry time, transaction count, JWT signature.
7. A URL is returned: either a full URL (`/get/{policyId}/{subscriberId}/{randomShareKey}`) or a short URL (`/datashare/{shortKey}`) depending on `mosip.data.share.urlshortner`.

### Retrieval Flow

**GET `/v1/datashare/get/{policyId}/{subscriberId}/{randomShareKey}`** (or `/datashare/{shortKey}`)
1. Reads object metadata from the object store.
2. Checks `transactionsallowed` — decrements by 1 if > 0; allows unlimited access if set to `-1`.
3. Returns the raw bytes in the response body with a `Signature` response header containing the JWT.
4. Throws `DataShareExpiredException` (quota exhausted) or `DataShareNotFoundException` (key missing).

### Key Components

| Class | Location | Purpose |
|---|---|---|
| `DataShareController` | `controller/` | REST endpoints — create and get |
| `DataShareServiceImpl` | `service/impl/` | Orchestrates policy lookup, encryption, signing, storage |
| `PolicyUtil` | `util/` | Partner Management Service client; caches policy responses |
| `EncryptionUtil` | `util/` | Cryptomanager client for partner-based encryption |
| `DigitalSignatureUtil` | `util/` | Keymanager JWT signing client |
| `CacheUtil` | `util/` | Spring `@Cacheable` wrapper for short-URL → full-key mapping |
| `DataShareBeanConfig` | `config/` | Wires `S3Adapter`, `TaskExecutor`, `ObjectMapper`, `RestUtil` |
| `DataShareExceptionHandler` | `controller/handler/` | `@ControllerAdvice` — maps domain exceptions to HTTP responses |

### External Service Dependencies

| Service | Integration Point | `ApiName` enum value |
|---|---|---|
| Partner Management | Policy lookup by `policyId` + `subscriberId` | `PARTNER_POLICY` |
| Cryptomanager | Encrypt data for partner-based encryption | `CRYPTOMANAGER_ENCRYPT` |
| Keymanager | JWT sign the payload digest | `KEYMANAGER_JWTSIGN` |
| Keymanager | Certificate fetch / upload | `KEYMANAGER_GET_CERTIFICATE`, `KEYMANAGER_UPLOAD_OTHER_DOMAIN_CERTIFICATE` |
| Object Store | Binary blob + metadata (S3-compatible) | `khazana` `S3Adapter` |

`RestUtil` wraps Spring `RestTemplate` for all outbound REST calls. URL values for each `ApiName` are resolved from environment properties (e.g., `PARTNER_POLICY`, `CRYPTOMANAGER_ENCRYPT`) provided by the config server.

### Async Thread Pool

Encryption and JWT signing are parallelised per request using a `ThreadPoolTaskExecutor` bean (`dataShareTaskExecutor`). The pool degrades gracefully under saturation via `CallerRunsPolicy`. Sizes are tunable:

```
mosip.data.share.async.core-pool-size=25   # default
mosip.data.share.async.max-pool-size=50
mosip.data.share.async.queue-capacity=80
```

## Standalone Mode

Standalone mode bypasses Partner Management and Keymanager. Set these properties:

```properties
mosip.data.share.standalone.mode.enabled=true
mosip.data.share.static-policy.policy-json={"typeOfShare":"","transactionsAllowed":"2","shareDomain":"datashare.datashare","encryptionType":"NONE","source":"","validForInMinutes":"30"}
mosip.data.share.static-policy.policy-id=<must match policyId in /create request>
mosip.data.share.static-policy.subscriber-id=<must match subscriberId in /create request>
mosip.data.share.signature.disabled=true
```

`transactionsAllowed` of `-1` grants unlimited downloads. The `/create` API accepts an optional `usageCountForStandaloneMode` query parameter that overrides the configured count at request time (must be ≥ 1 or exactly -1).

**Standalone mode is not safe for production** — it disables policy verification and signature computation.

## Key Configuration Properties

All runtime properties come from the Spring Cloud Config Server:

| Property | Description |
|---|---|
| `mosip.data.share.urlshortner` | `true` → return short `/datashare/{key}` URL; `false` → full `/get/{policyId}/{subscriberId}/{key}` |
| `mosip.data.share.protocol` | `http` or `https` prefix for constructed URLs |
| `mosip.data.share.key.length` | Byte length of the random key appended to the share key (default `8`) |
| `mosip.data.share.policy-cache.expiry-time-millisec` | How often the policy cache is evicted (Spring `@Scheduled` fixed rate) |
| `mosip.data.share.prependThumbprint` | Whether to prepend the key thumbprint to encrypted output |
| `mosip.data.share.includeCertificate` | Include cert in JWT signing request |
| `mosip.data.share.includeCertificateHash` | Include cert hash in JWT signing request |
| `mosip.data.share.includePayload` | Include payload in JWT signing request |
| `mosip.data.share.digest.algorithm` | Digest algorithm for JWT payload (default `SHA256`) |
| `data.share.application.id` | Application ID sent to Cryptomanager (default `PARTNER`) |
| `PARTNER_POLICY` | URL template for Partner Management policy API |
| `CRYPTOMANAGER_ENCRYPT` | URL for Cryptomanager encrypt API |
| `KEYMANAGER_JWTSIGN` | URL for Keymanager JWT sign API |

## CI/CD

GitHub Actions (`.github/workflows/push-trigger.yml`) triggers on pushes to `master`, `develop*`, `release*`, and `MOSIP*` branches:
1. Maven build via reusable `mosip/kattu` workflow (Java 21).
2. Publish JARs to Maven Central (OSSRH) — skipped for PRs, master, and releases.
3. Build and push `data-share-service` Docker image to Docker Hub.
4. SonarCloud analysis (project key: `mosip_durian`) — skipped for PRs.

Kubernetes deployment uses the Helm chart in `helm/datashare/` (requires Kubernetes 1.12+, Helm 3.1.0+).

## Testing Notes

- Unit tests use JUnit 4, Mockito, and PowerMock.
- Tests run against an H2 in-memory database; no external services are required.
- Surefire is configured with `--add-opens` JVM args for Java 21 module compatibility.
- SonarQube excludes `constant/`, `config/`, `dto/`, `entity/`, `repository/`, `httpfilter/`, and `*BootApplication` from coverage.
- The `TestBootApplication` in `src/test/java` bootstraps the test Spring context with a `TestSecurityConfig` that bypasses auth.
