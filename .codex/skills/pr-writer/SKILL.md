---
name: pr-writer
description: Use when creating or editing GitHub PR titles or bodies, especially with gh pr create/edit. Enforces this repository's PR title format and Korean PR body template.
---

# PR Writer

Use this skill before creating or editing any pull request title or body.

Root entrypoint: `AGENTS.md`.
Use this skill as the source of truth for PR title/body writing.

## Required Checks

1. Check relevant `.도윤/pr` documents when a PR planning document exists for
   the work.
2. Read `.github/PULL_REQUEST_TEMPLATE.md`.
3. Inspect the relevant diff, commits, or PR planning document before writing the
   body.
4. Use `.도윤/pr` as context only. Do not copy it verbatim.

## PR Title

- Format: `Type: Description`
- Do not include scope.
- Use English only.
- Use imperative mood.
- Capitalize the first letter of the type and description.
- Do not end with a period.
- Allowed types: `Feat`, `Fix`, `Add`, `Remove`, `Refactor`, `Docs`, `Chore`, `Test`, `Style`, `Implement`.

Examples:

- `Feat: Add Ingredient controller`
- `Fix: Handle invalid profile status`
- `Docs: Update PR template`

## PR Body

- Write the body in Korean.
- Preserve all sections from `.github/PULL_REQUEST_TEMPLATE.md`.
- Keep bullets concise and review-oriented.
- In `작업 사항`, summarize actual implementation units, not commit history.
- In `테스트 여부`, list commands that were actually run and their result.
- If a test was not run or failed, state the reason clearly.
- In `리뷰어에게 한마디`, mention only useful review context such as conflict risks, excluded scope, follow-up work, or environment issues. Write `없음` if there is nothing notable.

## GH CLI Usage

When using `gh pr create` or `gh pr edit`:

- Pass the title using the required `Type: Description` format.
- Fill the body from the repository template in Korean.
- Do not use the commit message as the PR title if it includes scope.
