---
name: smart-commit
description: Analyze git changes, split them into meaningful commit units, perform selective staging, and create commits following repository convention. Stop before push.
---

# Smart Commit Skill

## Purpose

This skill automates commit creation with high-quality structure.

It performs:

1. git diff analysis
2. semantic grouping of changes
3. selective staging (file or hunk level)
4. commit message generation based on repository convention
5. commit execution

This skill MUST NOT:

- push changes
- create pull requests
- merge branches

---

## Execution Flow

Follow this order strictly:

1. inspect current branch
2. run `git status`
3. run `git diff`
4. identify all modified / added / deleted files
5. group changes into commit units based on intention
6. generate commit plan
7. perform selective staging for one unit
8. generate commit message
9. run `git commit`
10. repeat for remaining units if clean separation exists
11. stop before push

---

## Commit Grouping Rules

### One Commit = One Intention

Each commit must represent exactly one logical purpose.

Valid intentions:

- feature addition
- bug fix
- refactor
- test update
- docs update
- formatting change
- removal

---

### Do NOT mix

Never combine in one commit:

- feature + refactor
- refactor + test
- test + docs
- multiple domains without clear single intention

---

### Grouping Heuristics

When analyzing `git diff`:

1. group by **primary intention**, not by file type
2. prefer **domain-based grouping**
3. separate test changes unless tightly coupled
4. separate docs unless trivial
5. if one file contains mixed changes:
   - prefer hunk-level staging
   - if unsafe → report before committing

---

## Selective Staging Rules

### Priority

1. file-level staging
2. hunk-level staging (when needed)
3. avoid `git add .` unless all changes share one intention

---

### Example

Bad:

```bash
git add .