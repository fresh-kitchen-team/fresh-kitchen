# Codex Behavior Rules

## Commit Workflow

- Always use the "smart-commit" skill for any git-related work.
- Never use `git add .` blindly.
- Always perform selective staging based on intention.
- Always split commits into meaningful units.

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