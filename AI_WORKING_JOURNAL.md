# AI Working Journal

## 2026-11-14 — Initial layered foundation

- **Prompt:** Create the project structure for the domain, repository, service, and related backend layers from the assessment brief.
- **AI contribution:** Read the brief and the existing README, then implemented a Java 21/Spring Boot layered modular monolith with the claim lifecycle, REST DTOs/controller, validation and error mapping, Spring Data JPA repository, H2 local configuration, README, and tests.
- **Decision and reasoning:** Retained the Java 21/Spring Boot choice already stated in the repository. Kept claim lifecycle actions synchronous over REST and persistence in one service so each state change remains transactional; deferred Kafka and service decomposition until consumers or scaling needs justify them.
- **Accepted/overridden:** No candidate review or override has been recorded yet. The candidate should review the generated changes and record any accepted, challenged, or changed decisions here before submission.
- **Validation:** `mvn -q test` passed, including claim lifecycle and API/persistence integration tests.

## 2026-11-15 — 16-character claim IDs

- **Prompt:** Resolve MySQL truncation on `claim_id` by limiting generated claim IDs to 16 characters, and update the project documentation and tests.
- **AI contribution:** Updated claim ID generation and JPA column metadata to use 16 uppercase hexadecimal characters from a UUID, and expanded unit, mocked service, and API/persistence integration coverage.
- **Decision and reasoning:** Retained UUID-based generation while storing its first 16 hexadecimal characters to fit the requested database width. Documented that this shortened identifier has lower collision resistance than a full UUID; the public reference remains `CLM-` plus the internal ID.
- **Validation:** `mvn -q -DforkCount=0 test` passed, including domain unit, mocked service, and API/persistence integration tests.

## 2026-11-15 — Architecture and workflow documentation

- **Prompt:** Document the technology stack, architecture, workflows, and project behavior.
- **AI contribution:** Added `docs/ARCHITECTURE.md` with Mermaid architecture, claim lifecycle, and local startup diagrams; technology stack, API, data model, source layout, and boundary descriptions. Linked it from the README and corrected the API path and project tree there.
- **Decision and reasoning:** Described implemented behavior rather than proposing Kafka or microservices as existing components. Kept diagrams in Mermaid so they are readable as source and render in compatible Markdown viewers.
- **Validation:** Cross-checked the documented routes, technology versions, configuration, ID format, and lifecycle against the source and build files. Documentation-only change.
