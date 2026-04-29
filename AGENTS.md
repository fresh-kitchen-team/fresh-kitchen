# Codex Behavior Rules

## Commit Workflow

- Always use the "smart-commit" skill for any git-related work.
- Never use `git add .` blindly.
- Always perform selective staging based on intention.
- Always split commits into meaningful units.

## Pull Request Workflow

- Always use the "pr-writer" skill for PR title/body creation or updates.
- Before creating or editing a PR, read `.github/PULL_REQUEST_TEMPLATE.md`.
- PR titles must follow `Type: Description`.
- PR titles must not include scope.
- PR bodies must be written in Korean and follow the repository PR template.

## Commit Convention

Follow strictly:

Type(Scope) : Description

Rules:
- English only
- Imperative mood
- First letter uppercase
- No period at the end

Allowed types:
Feat / Fix / Add / Remove / Refactor / Docs / Chore / Test / Style / Implement

## Automation Boundary

Allowed:
- git diff analysis
- selective staging
- commit creation

Not allowed:
- git push
- PR creation
- merge

## Default Behavior

When changes exist:
- analyze diff
- create commit plan
- execute smart commits

Do not wait for explicit instructions.

## Personal Ops Documents

- Personal working documents under `./.도윤/` are managed locally and ignored by git.
- Repo-wide rules stay in this file.
- If a task needs `.도윤` workflows, also read `./.도윤/AGENT.md`.
