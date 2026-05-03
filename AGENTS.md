# FreshKitchen AI Working Rules

This file is the entrypoint for AI agents working in this repository.
Keep detailed workflows in their source documents and use this file as the
project-level architecture guide and index.

## Project Shape

- Backend: Spring Boot 3.4.5, Java 17, Gradle.
- Persistence: Spring Data JPA, PostgreSQL, Flyway migrations.
- Testing: JUnit 5, Spring Boot test slices, Testcontainers for PostgreSQL.
- Main package: `com.example.freshkitchen`.

## Operating Principles

- Think before editing: identify the task goal, relevant files, and expected verification before changing code.
- Keep changes surgical: modify only files directly related to the requested task.
- Prefer simplicity: do not introduce new abstractions, layers, or dependencies unless the task clearly requires them.
- Preserve project conventions: follow existing package structure, naming, response contracts, and test patterns.
- Make work verifiable: explain how the change was checked through tests, build commands, or targeted reasoning.
- Stop at the requested boundary: do not push, open PRs, merge, or perform unrelated cleanup unless explicitly asked.

## Architecture Map

- `src/main/java/.../domain`: domain entities, enums, repositories, domain services,
  and domain exceptions. Keep business invariants here.
- `src/main/java/.../application`: use case interfaces, application services, and
  application DTOs. Coordinate domain objects and repositories here.
- `src/main/java/.../presentation`: controllers and request/response DTOs. Keep
  HTTP concerns out of domain/application layers.
- `src/main/java/.../global`: cross-cutting response, exception, and configuration
  code.
- `src/main/resources/db/migration`: Flyway SQL migrations. Treat committed
  migrations as immutable; add a new migration for schema changes.
- `src/test/java`: unit, slice, repository, and integration tests.

## Coding Rules

- Prefer existing project patterns over new abstractions.
- Do not use `@Setter` on JPA entities.
- Use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for entity default
  constructors.
- Change entity state through constructors, factories, or explicit business
  methods.
- Use Java `record` for DTOs where the local package pattern allows it.
- Do not expose JPA entities directly from APIs.
- Controllers should return the project success response contract through
  `ApiResponse`/`ResponseEntity<ApiResponse<T>>` when adding or refactoring API
  endpoints.
- Throw `BusinessException` subclasses with domain `ErrorCode` values for
  expected service/domain failures.
- Do not throw raw `RuntimeException` for expected business cases.
- Use `@RequiredArgsConstructor` constructor injection; do not use field
  `@Autowired`.
- Mark read-only service methods with `@Transactional(readOnly = true)`.
- Do not use `System.out.println`; use logging where runtime diagnostics are
  needed.
- Do not leave placeholder implementation code.

## Workflow Index

Use this table for routing only. After choosing a workflow skill, let that skill
own the detailed file-reading order. Do not pre-read downstream documents from
this table unless the task is specifically about that document.

| Task | Source of truth |
| --- | --- |
| General implementation work | This file first, then relevant source files |
| Commit analysis and commit creation | `.codex/skills/smart-commit/SKILL.md` |
| PR title/body writing | `.codex/skills/pr-writer/SKILL.md` |
| Personal operations and weekly notes | `.도윤/AGENT.md` |
| Commit and branch convention maintenance | `docs/git-convention.md` |
| Exception and error code policy | `docs/exception-spec.md` |

## Commit Workflow

- Always route git-related work to the `smart-commit` skill.
- The skill owns diff inspection, selective staging, commit splitting, and commit
  message rules.

## Pull Request Workflow

- Always route PR title/body creation or editing to the `pr-writer` skill.
- The skill owns the `.도윤/pr` context check, PR template read, and PR
  title/body rules.

## Automation Boundary

Allowed without extra user confirmation:

- git diff/status analysis
- selective staging
- commit creation

Not allowed unless the user explicitly asks:

- git push
- PR creation
- merge

## Default Git Behavior

When changes exist and the user asks for implementation or cleanup:

1. inspect branch and working tree
2. analyze the diff
3. create a commit plan
4. stage selectively
5. create smart commits
6. stop before push

## Personal Ops Documents

- Repo-wide architecture, coding, testing, and automation rules stay in this file.
- Personal planning, PR context, weekly notes, and operation logs stay under `./.도윤/`.
- Read `./.도윤/AGENT.md` only when the task explicitly involves personal operations, PR context, planning notes, or `.도윤` documents.
- Do not mix personal operation notes into repo-wide rules unless they become stable project conventions.