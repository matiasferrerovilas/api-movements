# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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
