# Git Commit / PR Convention

## 1. Purpose

This document defines commit message, branch naming, and pull request conventions.

The goals are:

- consistent commit history
- readable logs for developers
- predictable structure for collaboration
- compatibility with AI-based tooling and automation

---

## 2. Branch Strategy

feat/* → dev → main

### Branch Roles

- `feat/*`
  - feature or task-level branch
  - created per unit of work

- `dev`
  - integration branch
  - used after validation and review

- `main`
  - production-ready branch
  - deployment only

---

## 3. Branch Naming Convention

feat/domain-feature

### Rules

- lowercase only
- words separated by `-`
- short but meaningful
- reflect main purpose

### Examples

feat/user-auth
feat/bean-validation
feat/ingredient-management
feat/receipt-ocr-processing

---

## 4. Commit Message Convention

### 4.1 Format

Type(Scope) : Description

### Structure

- `Type`
  - category of change
  - uppercase first letter

- `Scope`
  - domain/module name
  - uppercase first letter

- `Description`
  - short summary in English
  - imperative form
  - no period at the end

---

### 4.2 Writing Rules

#### Language

- English only

#### Verb Style

- Use **imperative mood**

Correct:

Add(User) : Create login API
Fix(Token) : Prevent expired token access

Incorrect:

added login api
fixing bug
create login

---

### 4.3 Allowed Types

| Type       | Description                         |
|------------|-------------------------------------|
| Feat       | new feature                         |
| Fix        | bug fix                             |
| Add        | add code/resource                   |
| Remove     | delete code/resource                |
| Refactor   | internal improvement                |
| Docs       | documentation                       |
| Chore      | maintenance                         |
| Test       | test code                           |
| Style      | formatting only                     |
| Implement  | realization of planned logic        |

---

### 4.4 Type Usage Guide

#### Feat

Feat(Auth) : Support social login

#### Fix

Fix(Token) : Handle expired token case

#### Add

Add(User) : Add signup DTO

#### Remove

Remove(Auth) : Delete legacy token util

#### Refactor

Refactor(Bean) : Extract validation logic

#### Docs

Docs(Readme) : Add setup instructions

#### Chore

Chore(Gradle) : Update dependency version

#### Test

Test(User) : Add signup validation test

#### Style

Style(Api) : Reformat controller code

#### Implement

Implement(Receipt) : Apply OCR parsing logic

---

### 4.5 Scope Rules

Scope represents the main affected domain.

#### Examples

User
Auth
Bean
Receipt
Ingredient
Fridge
Api
Security
Readme
Gradle

#### Guidelines

- use one primary scope
- use domain-level naming
- avoid overly long scope names

---

## 5. Pull Request Convention

### Format

type: one-line summary

### Rules

- lowercase `type`
- English only
- concise summary
- no unnecessary punctuation

### Examples

feat: implement user signup flow
fix: resolve token validation bug
refactor: simplify bean validation logic
docs: update branch strategy

---

## 6. Commit vs PR Roles

### Commit

- smallest logical change unit

Add(User) : Create signup DTO
Fix(User) : Handle duplicate email

### PR

- summarizes entire feature branch

feat: implement user signup flow

---

## 7. Best Practices

### One Commit = One Intention

Good:

Add(Auth) : Create JWT provider
Test(Auth) : Add JWT test

Bad:

Add(Auth) : Create JWT provider and fix test and update docs

---

### Do Not Mix Concerns

Avoid mixing:

- feature logic
- refactoring
- tests
- documentation
- formatting

---

### Be Specific

Good:

Fix(Auth) : Reject malformed bearer token

Bad:

Fix(Auth) : Fix issue

---

## 8. Rules for AI and Automation

To ensure machine readability:

- always use exact format:

Type(Scope) : Description

- consistent capitalization
- no punctuation variation
- no mixed intentions
- PR format must follow:

type: summary

### Benefits

- automated commit parsing
- changelog generation
- AI-assisted review
- consistent repository structure

---

## 9. Summary

### Commit

Type(Scope) : Description

### PR

type: summary

### Branch

feat/domain-feature