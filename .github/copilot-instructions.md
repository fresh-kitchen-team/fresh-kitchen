# 🤖 AI Code Review & Generation Guidelines

When performing a code review or generating code suggestions, respond in Korean.
When performing a code review, focus on readability and avoid nested ternary operators.

---

# Project Architecture Rules
When writing or reviewing code for this project, you MUST strictly adhere to the following architectural conventions:

## 1. Entity Rules (DDD)
- NEVER use `@Setter` on JPA `@Entity` classes. 
- Use `@Builder` on a constructor, or create explicit business methods (e.g., `updateProfile()`) for state changes.
- Ensure the default constructor is `protected` (`@NoArgsConstructor(access = AccessLevel.PROTECTED)`).

## 2. DTO (Data Transfer Object) Rules
- ALWAYS use Java `record` for DTOs to guarantee immutability.
- Group Request and Response DTOs as Inner Records inside a domain-specific wrapper class (e.g., `public class UserDto { public record SignUpReq(...) {} }`).
- Never expose raw JPA Entities directly to the API response.

## 3. API Response Format
- Every Controller endpoint MUST return a `ResponseEntity<ApiResponse<T>>`, utilizing the global `ApiResponse.onSuccess()` format.

## 4. Exception Handling & Logging
- Do not throw raw Java exceptions (like `RuntimeException`) in Service logic.
- Always throw a custom `BusinessException` with the appropriate `ErrorCode` enum.
- Do not use `System.out.println()`. Use `@Slf4j` and log errors via `log.error()`.

## 5. Dependency Injection & Output Quality
- ALWAYS use Constructor Injection via `@RequiredArgsConstructor`. Never use `@Autowired` on fields.
- Read-only service methods MUST be annotated with `@Transactional(readOnly = true)`.
- Do NOT output placeholder code (e.g., `// TODO: implement here`). Provide fully functional, complete copy-pasteable code.
