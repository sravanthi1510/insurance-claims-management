# Insurance Claims Management

Java 21 and Spring Boot backend for the Chubb APAC claims assessment. The first implementation is a layered modular monolith: claims are the initial domain boundary, while HTTP is used for interactive claim submission and lifecycle actions. Events/Kafka and independently deployed services are intentionally deferred until there is a concrete asynchronous integration or scaling need.

## Project structure

See [Architecture and workflows](docs/ARCHITECTURE.md) for the technology stack, architecture and lifecycle diagrams, local startup flow, API details, and implementation boundaries.

```text
src/main/java/com/chubb/claims/
  ClaimsApplication.java
  claim/
    controller/           # REST controller
    api/dto/              # Validated request and response contracts
    api/mapper/            # Entity-to-response mapping
    domain/               # Claim statuses and markets
    entity/               # Claim aggregate, persistence mapping, lifecycle
    repository/           # Spring Data persistence boundary
    service/              # Transactional use cases
  config/                 # Database credential loading
  exception/              # HTTP exception handling and error response
src/main/resources/
  application.properties # MySQL and LocalStack Secrets Manager configuration
src/test/java/
  com/chubb/claims/
    claim/domain/         # Claim lifecycle and ID unit tests
    claim/service/        # Mocked service tests
    claim/api/            # HTTP and persistence integration
```

## Run locally

Requires Java 21, Maven 3.9+, and Docker Compose:

```shell
docker compose up --build -d --wait
mvn spring-boot:run
mvn test
```

The local stack runs MySQL and LocalStack. On startup, LocalStack creates the `insurance-claims/database` secret; the application retrieves its `username` and `password` fields from Secrets Manager before configuring the datasource. Local development defaults are `claims_app` / `local-dev-password`; override them with `DB_USERNAME`, `DB_PASSWORD`, and `DB_CREDENTIALS_SECRET_NAME` if needed. The application uses `AWS_ENDPOINT_URL` and `AWS_REGION` to connect to LocalStack. Tests use an isolated in-memory H2 database and do not require Docker.

## Initial API

All endpoints are under `/api/v1/claims`:

| Method | Path | Purpose |
|---|---|---|
| POST | `/submit` | Submit a claim |
| GET | `/` | List claims; optional `?status=UNDER_REVIEW` filter |
| GET | `/{id}` | Retrieve a claim by internal ID |
| GET | `/reference/{reference}` | Track status and requested actions by public reference (without claimant PII) |
| POST | `/{id}/review` | Assign an officer and start review |
| POST | `/{id}/information-requests` | Ask the claimant for information |
| POST | `/{id}/additional-information` | Provide requested information |
| POST | `/{id}/settlement` | Settle a claim |
| POST | `/{id}/rejection` | Reject a claim |

Claims progress through `SUBMITTED → UNDER_REVIEW → INFORMATION_REQUIRED → UNDER_REVIEW → SETTLED/REJECTED`. Settlement and rejection are only allowed during review. Invalid lifecycle transitions return HTTP 409; missing claims return 404 and invalid requests return 400.

Each claim receives a 16-character uppercase hexadecimal ID derived from a UUID. The ID is used as the internal primary key and fits the `CLAIM_ID` column configured with length 16; this shortened ID has less collision resistance than a full UUID. The public claim reference is `CLM-` followed by that ID.

## Deliberate first-pass boundaries

- One relational store and one deployable application keep the assessment runnable and make lifecycle changes transactional.
- `Claim` owns lifecycle rules; the service coordinates use cases; the repository isolates persistence; the controller owns HTTP contracts.
- Optimistic versioning protects concurrent claim updates. Status and market values are persisted as strings.
- Reference-based tracking returns a deliberately limited response; full claim details still require an internal-ID endpoint. Authentication and authorization remain production requirements.
- The local application uses MySQL with schema auto-update; automated tests use H2. Neither schema auto-update nor the local setup is a production deployment strategy. Production should add database migrations, authentication/authorization, audit history, market-specific policy rules, and a managed database.
- The list endpoint is intentionally basic. Team workload, liability reporting, pagination, and asynchronous domain events are natural next steps once their access patterns and consumers are defined.
