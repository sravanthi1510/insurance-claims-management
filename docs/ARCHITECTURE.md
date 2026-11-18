# Architecture and workflows

This document describes the application as currently implemented. It is a synchronous Spring Boot modular monolith with a React customer/agent portal; it does not currently use a message broker or separate microservices.

## Technology stack

| Area | Technology | Role |
|---|---|---|
| Language/runtime | Java 21 | Application and tests |
| Application framework | Spring Boot 3.4.5 | Application lifecycle and dependency injection |
| HTTP API | Spring MVC | Versioned REST endpoints under `/api/v1/claims` |
| Authentication | Spring Security HTTP session | Role-aware customer/agent login; BCrypt password hashes |
| Web portal | React 18, Vite | Customer submissions/tracking and agent queue/review actions |
| Input validation | Jakarta Bean Validation | Validates request DTOs at the API boundary |
| Persistence | Spring Data JPA / Hibernate | Repository abstraction and ORM mapping |
| Runtime database | MySQL 8.4 | Local application persistence |
| Test database | H2 in-memory | Isolated integration-test persistence |
| Build and tests | Maven, Spring Boot Test, JUnit 5, Mockito | Build, unit, mock, and integration tests |
| Local orchestration | Docker Compose | Starts MySQL |

## Architecture

```mermaid
flowchart LR
    Browser[Customer or agent browser]
    Portal[React portal<br/>Vite development server]
    subgraph App[Spring Boot application - modular monolith]
        AuthController[AuthController]
        AuthService[AuthService]
        ClaimController[ClaimController]
        ClaimService[ClaimServiceImpl]
        Mapper[ClaimMapper]
        Aggregate[Claim entity / aggregate]
        ClaimRepository[ClaimRepository]
        CustomerRepository[CustomerAccountRepository]
        AgentRepository[AgentAccountRepository]
        Errors[Global exception handler\nHTTP error mapping]
        AuthController --> AuthService
        ClaimController --> ClaimService
        ClaimController --> Mapper
        ClaimService --> Aggregate
        ClaimService --> ClaimRepository
        ClaimRepository --> Aggregate
        AuthService --> CustomerRepository
        AuthService --> AgentRepository
        AuthController -. errors .-> Errors
        ClaimController -. errors .-> Errors
    end
    MySQL[(MySQL 8.4 server)]
    ClaimsDB[(insurance_claims_db<br/>claims)]
    CustomerDB[(customer_db<br/>customer_db.app_user)]
    AgentDB[(agent_db<br/>agent_db.app_user)]
    Browser --> Portal
    Portal -->|JSON requests and session cookie| AuthController
    Portal -->|JSON requests and session cookie| ClaimController
    Mapper -->|JSON response DTO| Portal
    ClaimRepository --> ClaimsDB
    CustomerRepository --> CustomerDB
    AgentRepository --> AgentDB
    ClaimsDB --- MySQL
    CustomerDB --- MySQL
    AgentDB --- MySQL
```

The controller owns HTTP routing and request/response concerns. `ClaimServiceImpl` coordinates use cases and transaction boundaries. The `Claim` entity enforces lifecycle transitions. `ClaimRepository` is the persistence boundary, and `ClaimMapper` maps entities to API DTOs. The reference-tracking route returns a limited DTO that excludes claimant personal information.

Customer and agent credentials are stored in separate MySQL databases on the same local MySQL server. `CustomerAccount` maps to the `customer_db` catalog and `AgentAccount` maps to the `agent_db` catalog; claims map to `insurance_claims_db`. The API authenticates with an HTTP session, hashes passwords with BCrypt, and checks roles and claim ownership on protected routes. Registration checks for the normalized email in both account repositories before saving to the selected role's database. Agent registration also checks the configured invitation code; set `AGENT_REGISTRATION_CODE` to a private value outside local development. Submissions in `SUBMITTED` status are available to agents, and taking one assigns it to the signed-in agent.


Registration is intentionally global across both account stores: the same email cannot be registered once as a customer and again as an agent. The lookup is case-insensitive. Sign-in remains role-specific and only searches the database associated with the role selected on the sign-in form.

## Claim request and response data flow

```mermaid
sequenceDiagram
    actor User as Customer or claims agent
    participant Portal as React portal
    participant Security as Spring Security session and role checks
    participant Controller as ClaimController
    participant Service as ClaimServiceImpl
    participant Entity as Claim entity
    participant Repository as ClaimRepository
    participant DB as insurance_claims_db
    participant Handler as Global exception handler

    User->>Portal: Submit claim or choose a claim action
    Portal->>Security: Send JSON request with session cookie
    Security->>Security: Verify session and required role
    Security->>Controller: Authenticated request
    Controller->>Controller: Validate input and claim ownership
    Controller->>Service: Run requested use case
    Service->>Repository: Load or save claim
    Repository->>DB: SELECT / INSERT / UPDATE
    DB-->>Repository: Claim record
    Repository-->>Service: Claim entity
    Service->>Entity: Apply allowed lifecycle transition
    Entity-->>Service: Updated claim state
    Service->>Repository: Save updated claim
    Repository->>DB: UPDATE claim
    DB-->>Repository: Persisted state
    Service-->>Controller: Updated claim
    Controller-->>Portal: JSON response DTO and status
    Portal-->>User: Refresh claim details and status

    alt Invalid input, missing claim, denied access, or invalid transition
        Controller->>Handler: Propagate application exception
        Handler-->>Portal: 400, 403, 404, or 409 API error
    end
```

The browser sends the session cookie with each protected request. The controller derives the customer's email or agent identity from the authenticated principal instead of trusting a client-supplied identity. For claim details, customers can access claims associated with their account; agents can access assigned claims and eligible unassigned submissions. Reference lookups are authenticated and return the limited `ClaimTrackingResponse`.

## Customer and agent portal workflow

```mermaid
flowchart LR
    Portal[React portal] --> Login{Sign in or create account}
    Login -->|Customer| CustomerAuth[customer_db]
    Login -->|Agent + invitation code| AgentAuth[agent_db]
    CustomerAuth --> CustomerUI[Customer workspace]
    CustomerUI --> Submit[Submit claim]
    Submit --> API[Spring Boot API]
    API --> ClaimsDB[(insurance_claims_db)]
    CustomerUI --> Track[View own claim status]
    Track --> API
    AgentAuth --> AgentUI[Agent workspace]
    AgentUI --> Queue[Assigned claims + unassigned submissions]
    Queue --> API
    AgentUI --> Actions[Take claim, request info,\nsettle or reject]
    Actions --> API
    API --> ClaimsDB
```

## Claim submission and lifecycle workflow

```mermaid
sequenceDiagram
    actor Client
    participant API as ClaimController
    participant Service as ClaimServiceImpl
    participant Claim as Claim aggregate
    participant Repo as ClaimRepository
    participant DB as MySQL / H2

    Client->>API: POST /api/v1/claims/submit (validated JSON)
    API->>Service: submit(request)
    Service->>Claim: create claim
    Note over Claim: Generate 16 uppercase hex characters from UUID\nReference = CLM- + claim ID\nInitial status = SUBMITTED
    Service->>Repo: save(claim) [transaction]
    Repo->>DB: INSERT claim
    DB-->>Repo: persisted row
    Repo-->>Service: saved claim
    Service-->>API: claim
    API-->>Client: 201 Created + Location + response DTO

    Client->>API: POST /{id}/review
    API->>Service: startReview(id, authenticatedAgentName, authenticatedAgentEmail)
    Service->>Repo: load claim
    Repo->>Claim: SUBMITTED -> UNDER_REVIEW
    Service->>Repo: save(claim) [transaction]
    Repo->>DB: UPDATE claim
    API-->>Client: 200 OK + updated DTO

    opt More information required
        Client->>API: POST /{id}/information-requests
        API->>Service: requestInformation(id, reason)
        Service->>Claim: UNDER_REVIEW -> INFORMATION_REQUIRED
        Service->>Repo: save(claim)
        API-->>Client: 200 OK
        Client->>API: POST /{id}/additional-information
        API->>Service: provideAdditionalInformation(id, information)
        Service->>Claim: INFORMATION_REQUIRED -> UNDER_REVIEW
        Service->>Repo: save(claim)
        API-->>Client: 200 OK
    end

    alt Claim accepted
        Client->>API: POST /{id}/settlement
        API->>Service: settle(id, reason)
        Service->>Claim: UNDER_REVIEW -> SETTLED
        Service->>Repo: save(claim)
        API-->>Client: 200 OK
    else Claim declined
        Client->>API: POST /{id}/rejection
        API->>Service: reject(id, reason)
        Service->>Claim: UNDER_REVIEW -> REJECTED
        Service->>Repo: save(claim)
        API-->>Client: 200 OK
    end
```

Allowed transitions are `SUBMITTED -> UNDER_REVIEW -> INFORMATION_REQUIRED -> UNDER_REVIEW -> SETTLED` or `REJECTED`. Settlement and rejection require `UNDER_REVIEW`. Invalid transitions are reported as HTTP 409; a missing claim returns 404, and invalid request data returns 400.

## Startup and local environment workflow

```mermaid
flowchart TD
    Compose[ docker compose up --build -d --wait ]
    Compose --> MySQL[MySQL 8.4 starts\nclaims + customer + agent databases]
    MySQL --> Boot[Start Spring Boot application]
    Boot --> Ready[Serve REST API]

    Tests[mvn test] --> H2[H2 in-memory test database]
    H2 --> TestRun[Run isolated tests without Docker]
```

Docker Compose initializes MySQL and its three databases. The Spring Boot application creates the customer and agent account tables at startup. The React development server proxies API calls to the backend; run it separately with `npm run dev`.

## REST API

Authentication endpoints are relative to `/api/v1/auth`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/register` | Register a customer or agent; duplicate email returns 409 |
| `POST` | `/login` | Authenticate against the account store selected by role |
| `GET` | `/session` | Return the current authenticated user's profile |
| `POST` | `/logout` | Invalidate the current HTTP session |

All paths are relative to `/api/v1/claims`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/submit` | Submit a claim; returns 201 and a `Location` header |
| `GET` | `/` | List claims; optional `?status=UNDER_REVIEW` |
| `GET` | `/{claimId}` | Retrieve a claim by internal ID |
| `GET` | `/reference/{reference}` | Authenticated reference lookup returning limited claim information |
| `POST` | `/{id}/review` | Assign the signed-in agent and start review |
| `POST` | `/{id}/information-requests` | Request additional information |
| `POST` | `/{id}/additional-information` | Submit requested information |
| `POST` | `/{id}/settlement` | Settle a reviewed claim |
| `POST` | `/{id}/rejection` | Reject a reviewed claim |
| `GET` | `/agent/queue` | List claims available to or assigned to the signed-in agent |

## Identifier and persistence notes

The internal `claimId` is the first 16 hexadecimal characters of a random UUID, stored uppercase, and mapped to `CLAIM_ID` with length 16. The public reference is `CLM-` followed by the internal ID. Truncating a UUID to 16 hexadecimal characters reduces collision resistance compared with storing the full UUID; the database uniqueness constraint is still enforced. Claims use optimistic locking via the `VERSION` field. Runtime schema configuration is `spring.jpa.hibernate.ddl-auto=update`; tests use `create-drop` against H2.

## Source layout

```text
src/main/java/com/chubb/claims/
  ClaimsApplication.java
  auth/
    controller/       # Registration, sign-in, session, and sign-out
    domain/           # Account roles and authenticated principal
    dto/              # Authentication request and response DTOs
    entity/           # Customer and agent account mappings
    repository/       # Role-specific Spring Data JPA repositories
    service/          # Registration and authentication rules
  claim/
    controller/       # REST controller
    api/dto/          # Request and response DTOs
    api/mapper/       # Entity-to-DTO mapping
    domain/           # Status and market types
    entity/           # Claim persistence model and lifecycle behavior
    repository/       # Spring Data repository
    service/          # Use-case contract and implementation
  config/             # Secrets Manager startup integration
  exception/          # API exception handling
src/test/java/com/chubb/claims/
  claim/domain/       # Lifecycle and ID unit tests
  claim/service/      # Mocked service tests
  claim/api/          # REST and persistence integration tests
  config/             # Startup-configuration tests
```

## Build and verification

```shell
mvn test
```

Integration tests use H2 and MockMvc, so no Docker services are required for the test suite. For local runtime, start infrastructure and then the application:

```shell
docker compose up --build -d --wait
mvn spring-boot:run
```

## Current boundaries and follow-up architecture

This is one deployable application and one MySQL server with three databases: claims, customer accounts, and agent accounts. Database migrations, audit history, pagination, production identity management, MFA, session hardening, and market-specific policy rules are not currently implemented. Kafka or other asynchronous messaging and separately deployed services are intentionally outside the current design; they can be introduced when a concrete integration or scaling requirement exists.
