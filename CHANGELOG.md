# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.10.0] - 2026-09-01

### Added
- Credit-card installment purchases (`CREDITO`) now auto-generate their remaining cuotas month by
  month instead of only ever showing up as a gasto in the month they were charged. New
  `CreditInstallmentJob` (`@Scheduled`, same 1st-of-month pattern as `RecurringIncomeJob`) queries
  the previous month's `CREDITO` movements still short of `cuotasTotales` and clones each one with
  `cuotaActual + 1` and this month's date — no purchase-grouping id needed, since each row already
  carries its own state and the previous-month window is what makes the next cuota unambiguous.
  Reuses `MovementAddService.saveSystemMovement` (same internal path `IncomeAddService` already
  uses for recurring income), so the generated cuota gets the same enrichment/event-publishing/
  WebSocket push as a normal one.
- `Movement` gained `lastCreditPayment` — the date of a `CREDITO` purchase's final cuota, computed
  once (`date + (cuotasTotales - cuotaActual)` months) the first time a cuota of that purchase is
  entered, and copied verbatim (never recalculated) by `CreditInstallmentJob` on every subsequent
  cuota it generates — recalculating per-clone from that clone's own date would still land on the
  correct value in practice (the math is invariant month-to-month), but copying is more robust
  against a manually-edited date ever throwing it off. Backfilled for all pre-existing `CREDITO`
  rows in the same migration, using each row's own `cuotaActual` rather than assuming 1.

### Fixed
- `CalculateBalanceService.getBalance()` (the home page's ingreso/gasto cards) only counted
  `DEBITO` as gasto — `getMonthlyEvolution`, `calculateRecoveryTime`, the category breakdown, and
  `MonthlySummaryService` all already included `CREDITO`, so the same period showed a different
  total gasto depending on which screen you looked at. Now uses the same `GASTO_TYPES` as the rest
  of this class.

## [2.9.1] - 2026-09-01

### Fixed
- `UserService.getMe()`/`getMe(Long)`'s `@Cacheable` key used
  `T(org.springframework.security.core.context.SecurityContextHolder)...` — a SpEL type
  reference, resolved via `Class.forName` at runtime. This app runs as a GraalVM native image, and
  a type not explicitly registered in the native-image reflection config throws
  `SpelEvaluationException: EL1005E: Type cannot be found` when looked up this way — even one
  actively used elsewhere in the app (Spring Security's own filter chain, in this case), 500ing
  endpoints as unrelated as `GET /v1/banks`. Never reproduced under tests, which run on a normal
  JVM with no such closed-world reflection restriction. Replaced with a
  `@userService.getAuthenticatedEmail()` bean-reference expression, which resolves through the
  `BeanFactoryResolver` instead of `Class.forName` and needs no reflection registration at all.
- Same native-image reflection gap, different mechanism: several record/event types are only ever
  serialized via `SimpMessagingTemplate.convertAndSend(topic, Object)` (WebSocket) or deserialized
  via `@RabbitListener` — neither path is visible to Spring's AOT MVC-controller scanning, so
  `NotificationRecord`, `MemberRemovedReceivedEvent`, `InvitationReceivedEvent`,
  `InvitationAcceptedReceivedEvent` and `WorkspaceInvitationDTO` were never registered for
  reflection either — confirmed live: `NotificationRecord` threw
  `UnsupportedFeatureError: Record components not available` the first time a subscription
  payment triggered a notification push. Added to the existing `WebBindingRuntimeHints`
  registrar (already covered `MovementRecord`/`SubscriptionRecord`/etc. for exactly this reason;
  these five were the gap).

## [2.9.0] - 2026-08-31

### Added
- `sendInvitation` now requires a `role` (`COLLABORATOR`/`READ_ONLY`) alongside the email list —
  passed straight through to api-identity, which applies it to the membership when the invitee
  accepts. `WorkspaceInvitationDTO`/`WorkspaceSentInvitationDTO` (REST responses) and the
  WebSocket-pushed invitation payload now carry it too.

## [2.8.0] - 2026-08-31

### Security
- `READ_ONLY` was enforced only in the fe-movements UI (hiding "crear movimiento"/"crear
  servicio") — no backend endpoint validated the caller's role, only membership. Added
  `WorkspaceQueryService.verifyCanWrite(workspaceId)`, which resolves the caller's role via the
  already-cached `UserService.getMe(workspaceId)` and rejects with 403 when it's `READ_ONLY` (or
  the caller isn't a member at all). Wired into every create/update/delete path across movements,
  budgets, goals, subscriptions, income, categories and currencies — `MembershipCheckAspect`
  (backing `@RequiresMembership`, used only by mutating methods) now calls it instead of the
  plain membership check, and the handful of `save`/`create` methods that resolve their workspace
  via `WorkspaceContextService.getActiveWorkspaceId()` (no existing entity to hang
  `@RequiresMembership` off yet) call it explicitly. Bank management was deliberately left
  untouched — it's a per-user list (`UserBank`), not workspace-scoped, so a workspace role
  doesn't apply to it. Reads are unaffected: READ_ONLY members can still see everything, only
  mutations are blocked.

## [2.7.0] - 2026-08-31

### Added
- Cached the two api-identity calls that fired on almost every request (`UserService.getMe()`/
  `getMe(workspaceId)`, `WorkspaceQueryService.verifyUserIsMemberOfWorkspace`) for 5 hours via a
  new Redis `IDENTITY_CACHE`, keyed per-caller (and per-workspace where relevant) off the
  authenticated principal's email. Previously every one of those calls was a synchronous,
  uncached, timeout-less HTTP round-trip to api-identity — if it hung even briefly, this app
  couldn't serve a single operation despite the caller's JWT still being valid. A revoked
  membership is never masked by the cache: `verifyUserIsMemberOfWorkspace` only caches the
  successful (granted) outcome, so a removal always re-checks against api-identity immediately.

### Changed
- `GET /v1/users/me` now enriches the response with `metadata.workspaceRole` — the caller's role
  in their default workspace (`DEFAULT_WORKSPACE` setting), resolved server-side so the frontend
  doesn't need a second call. Omitted (stays whatever api-identity returned, i.e. absent) when the
  user has no default workspace configured.
- `WorkspaceMemberDTO.Metadata` dropped `members: string[]` — it was fully redundant with
  `memberDetails` (which already has email plus userId/role, everything `members` had and more).

### Security
- `application-prod.yaml` hardcoded the RabbitMQ username/password in plain text
  (`api-movements`/`api-movements`) while `DB_USERNAME`/`DB_PASSWORD`/`REDIS_PASSWORD`/
  `CORS_ALLOWED_ORIGINS` right next to it were already env vars — same pattern already fixed in
  api-identity this round. Now `${RABBIT_USERNAME}`/`${RABBIT_PASSWORD}`, no default.

## [2.5.0] - 2026-08-30

### Fixed
- N+1 in `GET /v1/expenses`, the highest-traffic endpoint in the app: `categories`/`currency`/`bank`
  on `Movement` are all `LAZY`, and `getExpenseBy`'s JPQL had no `JOIN FETCH`/`@EntityGraph`, so
  mapping a page of results (`movementMapper.toRecord` touches `currency`/`bank`,
  `enrichMovementWithIcons` touches `categories`) could run up to 3N+1 queries. `getExpenseBy` now
  `LEFT JOIN FETCH`es `currency`/`bank` directly (safe with `Pageable` since both are *-to-one). The
  `@ManyToMany categories` can't be fetch-joined the same way without Hibernate paginating in memory
  ("firstResult/maxResults specified with collection fetch"), so it's hydrated instead by a second,
  page-scoped query (`findByIdInFetchingCategories`) that populates it on the very same managed
  entity instances via Hibernate's session identity map — 2 queries total per page, regardless of N.
- `GoalAddService.update` silently ignored an explicit `targetDate: null` — it only overwrote the
  field when non-null, so "clear the target date" (the documented way to remove it, used by both
  web's `EditGoalModal` and mobile's `goal-sheet.tsx`) was indistinguishable from omitting the field
  and did nothing. `targetDate` is now always applied as sent, unlike `name`/`targetAmount` which
  stay partial-update (null = leave unchanged).

### Security
- No rate limiting anywhere in this backend, unlike api-identity — one of the home-lab's
  internet-exposed services, with neither the bank-statement import endpoint nor the standard CRUD
  under any abuse protection. New `RateLimiterService` (same Redis-backed fixed-window design as
  api-identity's) backs two limits: a generous global one (`RateLimitInterceptor`, 200
  req/min/user, applied to all of `/v1/**` via a `HandlerInterceptor` so it doesn't need touching
  every controller individually) and a much stricter one specific to
  `MovementImportFileService.importMovementsByFile` (10/hour/user) — that endpoint writes the
  upload to disk and parses it entirely with PDFBox, not a light CRUD operation. New
  `RateLimitExceededException` (429, same shape as api-identity's).
- `/ws/**` was (and stays) `permitAll()` at the HTTP layer — required for SockJS's handshake/XHR
  fallback requests, which aren't the STOMP CONNECT frame itself — but nothing validated the STOMP
  frames flowing over the resulting session: no `ChannelInterceptor` checked CONNECT or SUBSCRIBE at
  all. Topics are addressed by workspace id or by another user's email/Keycloak subject in plain
  text (`/topic/movimientos/{workspaceId}/new`, `/topic/invitations/{email}/new`), so any client
  that opened the SockJS connection — authenticated or not — could subscribe to any topic and
  passively harvest movements, services, categories, notifications and invitations from any
  workspace. New `StompAuthChannelInterceptor` on the client-inbound channel: CONNECT now requires a
  valid Bearer JWT (same `JwtDecoder`/`JwtAuthenticationConverter` beans the HTTP filter chain
  already uses — fe-movements and movements-mobile already send `Authorization: Bearer <token>` as
  a STOMP connect header, so no client-side change was needed), and every SUBSCRIBE is checked
  against the connected user before being allowed through — a workspace-scoped topic requires
  membership (verified against api-identity), an email-scoped topic requires the destination email
  to match the caller's own, and `/topic/workspace/default/{subject}` requires the destination's
  Keycloak subject to match the caller's own JWT `sub` claim. A destination matching none of the
  known topic shapes is rejected by default rather than let through.

### Changed
- Removed the unused `spring-boot-starter-oauth2-authorization-server` and
  `spring-boot-starter-security-oauth2-client` Gradle dependencies — this service only ever acts as
  an OAuth2 resource server validating Keycloak JWTs, never issues tokens, and is never itself an
  OAuth2 client. Both classes were dead classpath/native-image weight; the actual resource-server
  classes in use (`JwtDecoder`, `NimbusJwtDecoder`, `.oauth2ResourceServer()`) were only reachable
  as a transitive dependency of the authorization-server starter, which no longer holds — replaced
  with the correct, minimal `spring-boot-starter-oauth2-resource-server` declared directly.
- `CategoryInsightRecord.currency` (`GET /v1/insights`) is now a resolved `CurrencyRecord {symbol,
  id}`, matching `GoalRecord`/`BudgetRecord`/`MovementRecord`, instead of a bare currency-symbol
  `String` — the only money-bearing response in the API that didn't already do this. The rendered
  value was already identical either way (the string was `Currency.symbol` all along), but the
  inconsistent shape meant Insights couldn't carry a currency id the way every other card can.
  `InsightService` resolves it via the existing `CurrencyRepository`/`CurrencyMapper`, falling back
  to `CurrencyRecord(symbol, null)` if the symbol doesn't match a stored `Currency` (shouldn't
  happen in practice — the symbol always originates from a resolved `WorkspaceCurrency`). fe-movements
  (`InsightsPanel.tsx`) and movements-mobile (`insights-panel.tsx`) updated to read
  `insight.currency.symbol`.

### Added
- `GET /v1/workspace/invitations/sent` and `DELETE /v1/workspace/invitations/{invitationId}` proxy
  api-identity's new sent-invitations endpoints (`IdentityClient.getSentInvitations`/
  `cancelInvitation`), so a workspace owner/collaborator can list invitations they sent and cancel a
  still-pending one before the recipient responds.
- Lightweight gamification: registration streaks + "budget met" badges, no points system.
  `GET /v1/gamification/streak` reports the authenticated user's consecutive-days streak in the
  active workspace, fed in real time by a new `StreakEventHandler` listening on the same
  `MovementRecord` creation event `BudgetThresholdEventHandler` already uses (so only genuine new
  movements count, never edits). A broken streak (no activity yesterday or today) is detected lazily
  on read — no scheduled job needed for that part. `GET /v1/gamification/badges` lists
  `BUDGET_MET` badges (workspace + category + period), awarded by a new `BudgetBadgeJob`
  (`@Scheduled`, same cron as `MonthlySummaryJob`, last day of month 23:00) once a month's spend is
  final — a budget can't be "cumplido" mid-period. Yearly running-total budgets (year set, month
  null) are intentionally excluded from monthly badge evaluation to avoid a redundant badge every
  month the running total stays low; "always active" (year/month both null) and specific-month
  budgets are the ones this rewards. New `user_streaks`/`badges` tables (migration 057). No frontend
  wired up yet.

### Fixed
- `GoalAddService.update`/`.contribute` and `BudgetAddService.update` crashed with a
  `ClassCastException` on every call (`PATCH /v1/goals/{id}`, `PATCH /v1/goals/{id}/contribute`,
  `PATCH /v1/budgets/{id}`) — `MembershipCheckAspect` casts `args[idParamIndex()]` to `Long` to
  resolve the workspace before the method runs, but `@RequiresMembership` defaults `idParamIndex`
  to 0, and all three methods take the DTO first and the `Long id` second (`update(dto, id)`,
  `contribute(dto, id)`). Fixed by adding `idParamIndex = 1` to all three, matching the same
  `(dto, id)` shape `MovementAddService.updateMovement` already annotates correctly. Invisible to
  the existing unit-test suite: `GoalAddServiceTest`/`BudgetAddServiceTest` instantiate the service
  directly (bypassing the Spring AOP proxy, so the aspect never runs), and
  `MembershipCheckAspectTest` fabricates the annotation with whatever `idParamIndex` the test
  chooses, so a mismatch between a method's real parameter order and its own annotation can't
  surface in either suite — only a real Spring-context call does.
- `InvitationPublishServiceWebSocket.onInvitationReceived` was publishing the raw RabbitMQ
  `InvitationReceivedEvent` (field `invitationId`) straight over STOMP, but the frontend caches it
  as the same shape `GET /v1/workspace/invitations` returns (`WorkspaceInvitationDTO`, field `id`).
  A live-pushed invitation therefore had `id: undefined` in the frontend cache, and accepting it
  sent `PATCH /workspace/invitations/undefined`, which api-movements' `@PathVariable Long
  invitationId` rejected with `MethodArgumentTypeMismatchException`. Now maps the event into a
  `WorkspaceInvitationDTO` (status `PENDING`) before publishing, matching the REST shape exactly.
  Also updated `BaseControllerIntegrationTest`'s WireMock stubs and
  `OnboardingControllerIntegrationTest`'s assertions, which still mocked/verified the old two-call
  `POST /v1/users` + `POST /v1/workspaces` pattern instead of the new `POST /v1/onboarding/start` —
  a gap from the atomic-onboarding change above that the integration suite (not run at the time)
  would have caught.

### Added
- Onboarding now calls api-identity's new `POST /v1/onboarding/start` instead of two separate
  requests (`POST /v1/users` then `POST /v1/workspaces`), so a failure between the two calls can no
  longer leave a user with no workspace. `IdentityClient.createLogInUser(UserToAdd)` removed
  (dead — no remaining callers); `UserAddService.createLogInUser` renamed to `buildUserToAdd` and is
  now a pure builder with no HTTP call. `WorkspaceAddService.createWorkspaces` (batch) removed, only
  reachable from the old onboarding path.
- Gateway support for kicking a workspace member: `DELETE /v1/workspace/{workspaceId}/members/{userId}`
  forwards to api-identity via a new `IdentityClient.removeMember`, and clears the removed user's
  `DEFAULT_WORKSPACE` setting if it pointed to that workspace (same cleanup `leaveWorkspace` already
  did for a self-initiated departure). Mirrored `WorkspaceMemberDTO.Metadata.memberDetails` so the
  frontend has each member's userId, not just their email.
- Consume two new RabbitMQ events from api-identity: `identity.invitation.accepted` (pushes
  `MEMBERSHIP_UPDATED` to `/topic/workspace/{id}/members/update` so open clients refresh the member
  list when someone joins) and `identity.member.removed` (pushes `WORKSPACE_LEFT` to
  `/topic/membership/{email}/remove` so a kicked user's client reacts live). Previously only
  invitation-sent existed on this exchange.
- Goals and Insights now publish real-time notifications via the existing `NotificationService`,
  the same mechanism budgets/subscriptions already use. `GoalAddService.contribute()` notifies
  (`SUCCESS`) the first time a contribution makes `currentAmount` reach `targetAmount`. New
  `InsightThresholdEventHandler` mirrors `BudgetThresholdEventHandler`'s stateless before/after
  transition trick — on each non-income movement, it computes the affected category's spending
  deviation just before and just after the movement (via a new `InsightService.evaluateCategory`
  overload) and notifies (`INFO`) only on the exact movement that crosses the ±25% threshold, with
  no schema changes or "already notified" tracking needed.

### Removed
- Investments feature entirely: `Investment`/`InvestmentType` entities, controllers, services, the
  Yahoo Finance valuation client, and the plazo-fijo calculator. Migration `055_drop_tables_investment.sql`
  drops the `investment` and `investment_type` tables (`044`-`047` are left untouched as history).
  `MembershipDomain.INVESTMENT`, `EventType.INVESTMENT_*`, and `UserSettingKey.DEFAULT_INVESTMENT_TYPE`
  removed accordingly.

## [2.3.0] - 2026-08-18

### Added
- Savings goals (`Goal`): create/list/update goals per workspace with a target amount, optional target
  date and manual "contribute" action; response includes a computed `progressPercent` capped at 100.
  New `GET/POST/PATCH/DELETE /v1/goals` endpoints with workspace membership checks.
- Spending insights endpoint (`GET /v1/insights`): flags categories whose current-month spend deviates
  more than ±25% from the trailing 6-month average, built on top of the monthly summary snapshot data.
- Financial projection endpoint (`GET /v1/projection`): extrapolates a conservative linear cash-flow
  trend (current cumulative balance + average monthly net × months out) for 0/3/6/12 months ahead.
- `demo` Spring profile with idempotent seed data (movements, budgets, a subscription, recurring
  income, and goals) for workspace id 1, guarded by `@Profile("demo")`.
- CI step to auto-tag `main` with `vX.Y.Z` (read from `build.gradle`) after a successful build, skipped
  if the tag already exists.
- Per-category monthly breakdown (`gastosPorCategoria`) added to the monthly summary snapshot payload,
  enabling the insights/projection features to build on existing snapshot data instead of recomputing
  from raw movements.

### Changed
- `MonthlySummaryByCurrencyRecord` now includes a `gastosPorCategoria` field (per-category totals for
  that currency/month).

## [2.2.0]
### Added
- Rate limiting on sensitive endpoints and assorted security hardening.
- Swagger/OpenAPI documentation refresh across controllers.

## [2.1.0]
### Added
- Multi-app / multi-workspace migration: movements, income, subscriptions, categories and budgets are
  now scoped by workspace instead of a single implicit account.
- Notifications for subscription overdue payments and budget threshold crossings.
- Onboarding flow fixes for new workspaces.

## [2.0.0]
### Added
- Multiple categories per movement (`movement_categories` join table), replacing the single
  `category_id` column on `movements`.
- Workspace-scoped currencies (`workspace_currencies`) and per-workspace category catalog
  (`workspace_categories`).
- "Time to recover a expense" calculator based on trailing average savings.
- Chart of monthly savings evolution alongside spend.

## [1.0.x] - [0.8.x]
### Added
- Initial iterations of the movement tracking domain: banks, bank-statement PDF import (BBVA,
  Galicia), recurring income, subscriptions/services with payment tracking, budgets (monthly/annual/
  one-time) with threshold alerts, investments with live Yahoo Finance valuation and a time-deposit
  calculator, shared workspaces with role-based invitations, WebSocket (STOMP/SockJS) push updates,
  Keycloak OAuth2/JWT authentication, Liquibase migrations, and Prometheus metrics.

[Unreleased]: https://github.com/matiasferrerovilas/api-movements/compare/v2.3.0...HEAD
[2.3.0]: https://github.com/matiasferrerovilas/api-movements/compare/v2.2.0...v2.3.0
