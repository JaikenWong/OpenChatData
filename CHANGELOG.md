# Changelog

## [Unreleased] — 2026-05-29

### Security (Phase 1 — Hardening)

- **SQL Injection Defense**: `QueryService.isValidReadOnlySQL` now uses JSqlParser AST validation. Only pure `SELECT` (and `WITH ... SELECT` CTEs) pass. `SHOW`/`DESCRIBE`/`EXPLAIN` are intentionally rejected to prevent inline-comment write smuggling (e.g. `SHOW /*!50000 DROP TABLE x*/`). Multi-statement SQL (unquoted `;`) is rejected.
- **Tenant Isolation**:
  - New `TenantContext` (ThreadLocal) populated by `JwtAuthenticationFilter` from JWT claims and cleared in `finally`.
  - New `TenantIsolationInterceptor` registered for `/api/{schema,history,audit,dictionary,masking}/{tenantCode}/**`; returns `403` if URL `tenantCode` ≠ JWT `tenantCode`.
  - `QueryController.ask` cross-checks `TenantContext.get()` against JWT `tenantCode` before executing.
- **Password Masking**: `TenantController` returns shallow copies with `password = "********"` for `GET`/`POST`/`PUT`. Original entity is never mutated, so cached/persisted state is not corrupted.
- **RBAC Wiring (transition stub)**: `QueryController` calls `RBACService.checkPermission(username, tenantCode, "query")` before queries. Stub allows when no roles are defined for the tenant; once roles exist, the TODO is to resolve `userId` from `(username, tenantCode)` and call `hasPermission`.
- **Engineering**: `GlobalExceptionHandler` (`@RestControllerAdvice`) added for `AccessDeniedException`, `IllegalArgumentException`, and generic `RuntimeException` mapping.

### Tests

- **Java live tests** (real API hits, env-gated):
  - `EmbeddingClientLiveTest` — single + batch embedding, empty input.
  - `LLMClientLiveTest` — chat ping-pong, NL→SQL with JOIN, code-fence stripping, semicolon trimming.
  - `RerankServiceLiveTest` — passthrough when disabled, descending-by-score when enabled (asserts `top score > 0` to catch silent 401 fallback).
  - `QdrantServiceLiveTest` — ensureCollection idempotency, upsert + search ordering, missing-collection returns `[]`.
  - `testutil/DotEnv.java` — shared `.env` loader for live tests.
- **Shell e2e tests** (assertion-driven, with PASS/FAIL summary):
  - `scripts/lib.sh` — assertions (`assert_eq`, `assert_contains`, `assert_http_ok`, `assert_ge`), HTTP helpers (status persisted via tmp-file across `$(...)` sub-shells), login/MySQL/backend wait helpers.
  - `scripts/test-mysql.sh` — rewritten with real assertions: schema sync verification, generated-SQL keyword checks (SUM/customers/orders), data-row count.
  - `scripts/test-eav-mvcc.sh` — MVCC snapshot isolation: assert pre-commit value = `256`, no dirty read, MySQL direct read confirms commit, post-commit = `1024`.
  - `scripts/test-windchill.sh` — PTC Windchill PLM model (parts/documents/cad_docs/change_notices/change_activities/part_doc_links/bom_items/lifecycle_log/iba_def/iba_val), 9 question categories including MVCC.
  - `scripts/test-windchill-advanced.sh` — 32 PLM-grade questions: multi-version latest-revision, BOM recursion, change-loop tracking, doc approval, supplier-cost EAV triangle, overdue changes, organization aggregates, **SQL injection attack assertions**.
  - `scripts/run-all-tests.sh` — driver running all suites with overall summary.

### Bug Fixes

- `.env` `RERANK_API_KEY` had a leading `3` typo (`33d3c9fb…` → `3d3c9fb…`) — `RerankServiceLiveTest` was silently falling through to `fallbackOriginalOrder` on `401`; fixed and asserted `top score > 0`.
- Direct MySQL probes in shell tests were missing `--default-character-set=utf8mb4`, causing Chinese attribute filters to silently return empty.

### Files Added

```
backend/scripts/lib.sh
backend/scripts/run-all-tests.sh
backend/scripts/test-windchill.sh
backend/scripts/test-windchill-advanced.sh
backend/src/main/java/com/openchat4u/auth/TenantContext.java
backend/src/main/java/com/openchat4u/config/TenantIsolationInterceptor.java
backend/src/main/java/com/openchat4u/config/WebMvcConfig.java
backend/src/test/java/com/openchat4u/llm/EmbeddingClientLiveTest.java
backend/src/test/java/com/openchat4u/llm/LLMClientLiveTest.java
backend/src/test/java/com/openchat4u/query/RerankServiceLiveTest.java
backend/src/test/java/com/openchat4u/schema/QdrantServiceLiveTest.java
backend/src/test/java/com/openchat4u/testutil/DotEnv.java
```

### Files Modified

```
backend/src/main/java/com/openchat4u/api/TenantController.java          (password mask)
backend/src/main/java/com/openchat4u/auth/JwtAuthenticationFilter.java  (TenantContext)
backend/src/main/java/com/openchat4u/query/QueryController.java         (RBAC + tenant cross-check)
backend/src/main/java/com/openchat4u/query/QueryService.java            (JSqlParser AST validation)
backend/src/main/java/com/openchat4u/rbac/RBACService.java              (checkPermission stub)
backend/scripts/test-eav-mvcc.sh                                        (assertions + MVCC verification)
backend/scripts/test-mysql.sh                                           (assertions)
.env.example / backend/.env.example
```

### Test Results

- `mvn test`: **94 / 94 PASS** (Qdrant 3 skipped when service down)
- `bash scripts/run-all-tests.sh`: **4 suites**, 92 assertions (LLM non-determinism may flap 1–2)
- SQL injection attempts (`DROP TABLE`, `UNION SELECT users`) verified rejected by JSqlParser; `parts` table survives.

### Known Gaps (Phase 2 / 3 — TODO)

- Frontend JWT storage migration (`localStorage` → `sessionStorage` or httpOnly cookie) — frontend directory layout not located in this session.
- Real RBAC resolution (`userId` from `username + tenantCode`).
- SSE streaming, multi-turn conversation API integration.
- Connection pool TTL + health checks.
- Pagination, API versioning, frontend build/lint pipeline.
- Password strength validation + login lockout policy enforcement.
