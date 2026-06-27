# Test Notes — BFF Spring Projects

> Generated from the `.md` files in both `spring.simple.bff` and `spring.oidc.bff` test packages.

---

## Two Projects, Two Authentication Models

| | `spring.simple.bff` | `spring.oidc.bff` |
|---|---|---|
| Auth mechanism | Username/password (form login + HTTP Basic) | OIDC / Okta |
| Users | Hard-coded in-memory: `user/password` → `ROLE_myuser`, `admin/password` → `ROLE_myadmin` | Okta tokens; groups mapped to roles by `customOidcUserService` |
| Auth injection | `@WithMockUser` or `httpBasic()` | `oidcLogin()` |
| Scopes | ❌ None | ✅ `SCOPE_foo`, `SCOPE_bar` |
| Logout response | 200 OK | 204 No Content |
| CSRF | Always active (both deploy modes) | Always active (both deploy modes) |

> **Auth injection** — inject an authenticated user into the MockMvc request without a real login flow.

> **CSRF in standalone mode** — CSRF is still needed even when frontend and backend share the same port. A malicious third-party page can still trigger a cross-origin POST that the browser will automatically accompany with the session cookie. Same-origin policy only prevents *reading* the response, not *sending* the request. The only alternative would be setting `SameSite=Strict` on the session cookie, which Spring does not do by default.

---

## Core Testing Concepts (both projects)

1. **`@SpringBootTest` + `@AutoConfigureMockMvc`** — loads the full Spring context including the real `SecurityFilterChain`. No actual HTTP server starts.

2. **`@SpringBootTest(properties = "febaseurl=http://localhost:4200")`** — spins up a separate Spring context to test CORS/CSRF behaviour that only activates in split-deployment mode.

3. **CSRF** — any POST/PUT/DELETE must include `.with(csrf())` or it gets a **403** before even hitting auth. `.with(csrf().useInvalidToken())` tests rejection.

4. **JSON assertions** — use `jsonPath("$.field").value(...)` and `jsonPath("$.array", hasItem("value"))`.

5. **Test naming** — `subject_condition_expectedOutcome` e.g. `testSecuredAdmin_AsUser_Returns403`.

---

## `spring.simple.bff` — Test Classes

### `HelloEndpointTest` (4 tests)
`GET /hello` — `permitAll()`. Returns the username string if logged in, empty string if anonymous.  
Uses `@WithMockUser` — just reads whatever name is in the security context.

> **Note:** `/hello` is a temporary diagnostic endpoint. It will be removed from both `spring.simple.bff` and `spring.oidc.bff` once no longer needed. These tests will be deleted along with it.

| Test | Why |
|---|---|
| `testHello_Unauthenticated_Returns200WithEmptyBody` | Baseline — `permitAll` means no auth required; empty body asserts the anonymous branch. |
| `testHello_AsUser_Returns200WithUsername` | `@WithMockUser(username="user")` → `.getName()` returns `"user"`. |
| `testHello_AsAdmin_Returns200WithUsername` | Same for `"admin"`. |
| `testHello_AsCustomUser_Returns200WithUsername` | **Controller isolation test only** — only possible because `@WithMockUser` bypasses the real auth stack. In a real app, a non-existent username would be rejected at login (401 from `InMemoryUserDetailsManager`) and never reach the controller. The test confirms the controller just reflects `authentication.getName()` and is not coupled to the user store. |

---

### `ShortProfileEndpointTest` (5 tests)
`GET /shortprofile` — `permitAll()`. Returns JSON `{loggedIn, name, roles}`.  
Key cases: anonymous → `loggedIn:false`; multi-role user → all roles appear; custom role → reflected from context (not hard-coded).

| Test | Why |
|---|---|
| `testShortProfile_Unauthenticated_Returns200WithLoggedInFalse` | SPA reads `loggedIn` to show/hide login button. |
| `testShortProfile_AsUser_ReturnsProfileWithUserRole` | Asserts `name=="user"` and `roles` contains `"ROLE_myuser"`. |
| `testShortProfile_AsAdmin_ReturnsProfileWithAdminRole` | Same for `admin` / `ROLE_myadmin`. |
| `testShortProfile_WithMultipleRoles_ReturnsAllRoles` | Guards against controller only returning the first authority. |
| `testShortProfile_WithCustomRole_ReturnsCustomRole` | **Controller isolation test only** — same reasoning as `testHello_AsCustomUser`. In a real app this user would never authenticate. The test confirms the controller reflects authorities directly from the security context and does not validate them against a hard-coded list. |

---

### `CheckPostEndpointTest` (6 tests)
`POST /checkpost` — `permitAll()` but CSRF-protected.  
**Key lesson: CSRF is active even on public POSTs.** Without `.with(csrf())` → 403 regardless of auth state.

| Test | Why |
|---|---|
| `testCheckPost_Unauthenticated_WithoutCsrf_Returns403` | No user, no CSRF → blocked. Confirms CSRF active on anonymous public endpoints. |
| `testCheckPost_Unauthenticated_WithCsrf_Returns200` | `.with(csrf())` → 200. Locks in response contract `$.message`. |
| `testCheckPost_AsUser_Returns200` | Auth + CSRF → 200. Confirms they are orthogonal. |
| `testCheckPost_AsAdmin_Returns200` | Same for admin. |
| `testCheckPost_WithEmptyJson_Returns200` | `{}` is valid for the `Abc` DTO — guards against deserialization error. |
| `testCheckPost_WithComplexJson_Returns200` | Richer body also accepted — no strict schema enforcement. |

---

### `FormLoginTest` (6 tests)
`POST /login` — exercises the **real** auth stack: `UsernamePasswordAuthenticationFilter` → `InMemoryUserDetailsManager` → `BCryptPasswordEncoder`.  
`@WithMockUser` is deliberately NOT used here. Custom `successHandler` returns **200** (not Spring's default 302 redirect).

| Test | Why |
|---|---|
| `login_ValidUserCredentials_Returns200` | Happy path. Custom handler returns 200 (SPA-friendly, not a redirect). |
| `login_ValidAdminCredentials_Returns200` | Confirms both in-memory accounts work. |
| `login_WrongPassword_Returns401` | Bad password → custom `failureHandler` calls `sendError(401)`. |
| `login_UnknownUser_Returns401` | Spring normalises `UsernameNotFoundException` to same 401 (avoids enumeration). |
| `login_EmptyCredentials_Returns401` | Guards against null/empty-string bug in username lookup. |
| `login_ValidCredentials_WithoutCsrf_Returns403` | Correct credentials but no CSRF → blocked before auth runs. CSRF is **intentionally not ignored** on `/login` — the Angular SPA already has the `XSRF-TOKEN` cookie from a prior GET, so it can always supply the token. Ignoring it would weaken the posture for no practical gain. |

---

### `HttpBasicAuthTest` (8 tests)
Uses `.with(httpBasic("user","password"))` which adds a real `Authorization: Basic ...` header.

**Note**:  will remove this later. Basic is not needed when using swagger bff extension even with swagger.   

| Test | Why |
|---|---|
| `securedUser_WithValidUserCredentials_Returns200` | Happy path + asserts body is `"ok"`. |
| `securedAdmin_WithValidAdminCredentials_Returns200` | Same for admin. |
| `securedAdmin_WithUserCredentials_Returns403` | `user` authenticated but lacks `ROLE_myadmin`. |
| `securedUser_WithAdminCredentials_Returns403` | Roles are not cumulative by default. |
| `securedUser_WithWrongPassword_Returns401` | Bad password → custom `authenticationEntryPoint` returns 401. |
| `securedUser_WithUnknownUser_Returns401` | Unknown username → same 401. |
| `securedUser_WithNoCredentials_Returns401` | No `Authorization` header on secured endpoint → 401. |
| `hello_WithNoCredentials_Returns200` | Public endpoint (`permitAll`) reachable without credentials. |

---

### `LogoutEndpointTest` (5 tests)
`POST /logout` — returns **200**. No `.deleteCookies("JSESSIONID")` configured (unlike `spring.oidc.bff`).

| Test | Why |
|---|---|
| `logout_Authenticated_Returns200` | Custom handler returns 200 (not a redirect). |
| `logout_Unauthenticated_Returns200` | Spring fires success handler even for anonymous logout. |
| `logout_WithoutCsrfToken_Returns403` | CSRF active on `/logout` (no ignore configured). |
| `logout_Authenticated_ViaHttpBasic_InvalidatesSession` | **3-step test:** (1) GET with `httpBasic` → real session; (2) POST `/logout`; (3) GET same session → 401. Uses `httpBasic` in step 1 because it creates a genuine `HttpSession` (unlike `@WithMockUser`). **TODO: will be removed once HTTP Basic support is removed from `SecurityConfiguration`.** |
| `logout_Authenticated_ViaFormLogin_InvalidatesSession` | Same 3-step session-invalidation guarantee, but step 1 uses `POST /login` with real credentials. Exercises the full `UsernamePasswordAuthenticationFilter` → `InMemoryUserDetailsManager` → `BCryptPasswordEncoder` → `successHandler` stack — exactly the flow the Angular SPA uses. Superior to the `httpBasic` variant for production-realism. |

---

### `ApiLogoutEndpointTest` (5 tests)
`GET /apilogout` — exists in **both** projects. Manually clears `SecurityContextHolder` and invalidates the `HttpSession`.

**Why `apilogout` was invented — the problem with `POST /logout`:**

Spring Security's built-in `/logout` requires a **form POST + CSRF token**. A browser link, a `<script>`-triggered navigation, or the Swagger UI BFF plugin cannot easily perform a credentialed POST — they navigate by GET. Two distinct callers with two different interaction models:

| Caller | How it logs out | Why |
|---|---|---|
| Angular SPA | `POST /logout` + CSRF token | SPA controls the request; can set headers and attach tokens |
| Swagger UI BFF plugin | `GET /apilogout?source=swagger` | Browser navigation — cannot POST with CSRF from a plugin link |

**What `apilogout` does in `spring.simple.bff`:**
- Is a `GET` → no CSRF token required
- Is a plain `@GetMapping` controller method — Spring's `LogoutFilter` (which handles `POST /logout`) **never runs** for this request, so `apilogout` must manually do `SecurityContextHolder.clearContext()` + `session.invalidate()` itself
- Returns immediately with **no redirect** — the `?source=swagger` param is read but not acted upon (redirect block is commented out)
- The Swagger UI BFF plugin handles its own post-logout navigation, just as Angular does after `POST /logout`
- `SpringdocConfig.java` hardwires `"logout": "http://localhost:8081/apilogout?source=swagger"` into the BFF security scheme extension

**Why no redirect is needed here:**
The Swagger UI BFF plugin (like Angular) is responsible for its own post-logout navigation — the server just needs to clear the session and acknowledge. A server-side redirect would actually be counterproductive for a JS plugin that wants to control its own flow.

**Contrast with `spring.oidc.bff`:**
The OIDC variant *does* redirect — it must pass `id_token_hint` to Okta's end-session endpoint (`/v1/logout?id_token_hint=...&post_logout_redirect_uri=...`) to terminate the Okta-side session. That is the original reason `apilogout` was invented. `spring.simple.bff` inherited the endpoint as a GET-accessible logout hook but has no Okta session to terminate, so the redirect block remains commented out.

No CSRF needed — CSRF only applies to state-changing methods (POST/PUT/DELETE/PATCH).

| Test | Why |
|---|---|
| `testApiLogout_Unauthenticated_Returns200` | `permitAll` + GET → anonymous access works; session clearance is a no-op. |
| `testApiLogout_AsUser_Returns200` | Also works when logged in — session is cleared. |
| `testApiLogout_AsAdmin_Returns200` | Same for admin. |
| `testApiLogout_WithSourceParameter_Returns200` | `source=frontend` param is accepted without error — even though it is not acted upon in this variant. |
| `testApiLogout_WithSwaggerSource_Returns200` | `source=swagger` — the exact call the Swagger UI BFF plugin makes; returns 200, no redirect. |

---

### `SecuredUserEndpointTest` / `SecuredAdminEndpointTest` (6 tests each)
Role-based access control.

**Key distinction: 401 = not authenticated, 403 = authenticated but wrong role.**  
Roles are **not** hierarchical — `ROLE_myadmin` does not imply `ROLE_myuser` and vice versa. This is a deliberate design choice; role hierarchy can be added via Spring's `RoleHierarchy` bean if needed.

| Test | Why |
|---|---|
| `testSecuredAdmin_Unauthenticated_Returns401` | Custom `authenticationEntryPoint` returns 401 (not 302 redirect). |
| `testSecuredAdmin_AsUser_Returns403` | Authenticated but lacks `ROLE_myadmin`. |
| `testSecuredAdmin_AsAdmin_Returns200` | Happy path + asserts body is `"ok"`. |
| `testSecuredAdmin_WithWrongRole_Returns403` | **Controller isolation test only** — `"otheruser"` / `"otherrole"` are fabricated and do not exist in `InMemoryUserDetailsManager`; they would be rejected at login in a real app. The test confirms the security rule rejects any authenticated user who lacks `ROLE_myadmin`, regardless of what role they hold. |
| `testSecuredAdmin_WithBothRoles_Returns200` | Authorities are cumulative — both roles grants access to both endpoints. |
| `testSecuredAdmin_AuthenticatedWithoutRole_Returns403` | Zero authorities → authenticated (not 401) but denied (403). |

---

### `SecuredProfileEndpointTest` (5 tests)
`GET /secured/profile` — security rule: `authenticated()`. **No controller is implemented.**

Pattern: security check happens before handler lookup.
- Unauthenticated → **401** (security blocks it)
- Authenticated → **404** (security passes, no handler found)

This tests the **security boundary** of a planned-but-not-yet-built endpoint.

Note: in the `spring.oidc.bff` project this test class was updated to use `oidcLogin()` (mock OIDC principal) instead of `@WithMockUser` so its coding style matches the other OIDC tests and avoids principal-type mismatches. The behaviour asserted remains the same (401 when unauthenticated, 404 when authenticated).

| Test | Why |
|---|---|
| `testSecuredProfile_Unauthenticated_Returns401` | Security fires before routing. |
| `testSecuredProfile_AsUser_PassesSecurity` | → 404 (not 403 or 401). |
| `testSecuredProfile_AsAdmin_PassesSecurity` | Any authenticated user satisfies `authenticated()`. |
| `testSecuredProfile_WithAnyRole_PassesSecurity` | Role not in user store still passes `authenticated()`. |
| `testSecuredProfile_AuthenticatedNoRoles_PassesSecurity` | Zero roles but still authenticated → passes. |

---

### `SpaCsrfTest` (6 tests)
Tests `SpaCsrfTokenRequestHandler` + `CookieCsrfTokenRepository`.  
Context: `@SpringBootTest(properties = "febaseurl=http://localhost:4200")`.

`SpaCsrfTokenRequestHandler` does two things:
1. In `handle()` calls `csrfToken.get()` **eagerly** → writes the cookie on every response including GETs.
2. In `resolveCsrfTokenValue()` routes `X-XSRF-TOKEN` header (SPA) vs `_csrf` param (form) to the right handler.

| Test | Why |
|---|---|
| `get_PublicEndpoint_SetsXsrfTokenCookie` | Eager loading — cookie appears even on GETs. |
| `get_PublicEndpoint_XsrfCookieIsNotHttpOnly` | `withHttpOnlyFalse()` must be set — Angular's JS needs to read the cookie. |
| `post_WithoutCsrfToken_Returns403` | Baseline: no token → blocked. |
| `post_WithCsrfHeader_Returns200` | `.with(csrf())` exercises header routing through `SpaCsrfTokenRequestHandler`. |
| `post_WithInvalidCsrfToken_Returns403` | `csrf().useInvalidToken()` — handler must not accept any non-empty value. |
| `post_WithTokenReadFromCookie_Returns200` | Full round-trip: GET to capture real cookie → POST with that value in header. Closest simulation of Angular's `HttpClient` interceptor. |

---

### `CorsDisabledModeTest` (3 tests)
Context: plain `@SpringBootTest` — no `febaseurl` set → CORS deny-by-default.

| Test | Why |
|---|---|
| `preflight_AnyOrigin_IsRejected` | No `CorsConfigurationSource` → OPTIONS preflight → 403. |
| `get_WithAnyOriginHeader_NoAllowOriginHeaderInResponse` | No `Access-Control-Allow-Origin` in response → browser blocks it. |
| `get_WithoutOriginHeader_Returns200` | Same-origin / server-to-server requests still work. |

---

### `CorsEnabledModeTest` (7 tests)
Context: `@SpringBootTest(properties = "febaseurl=http://localhost:4200")`.  
Configured origin: `http://localhost:4200`, `allowCredentials(true)`.

| Test | Why |
|---|---|
| `preflight_FromConfiguredOrigin_Returns200WithAllowOriginHeader` | Preflight from allowed origin → 200 + header echoed. |
| `preflight_FromUnknownOrigin_Returns403` | Allow-list enforced strictly. |
| `preflight_WithoutOriginHeader_IsNotRejectedByCors` | Not a CORS request — must not be blocked, must not add header. |
| `get_FromConfiguredOrigin_ResponseContainsAllowOriginHeader` | Actual request after preflight must also carry the header. |
| `get_FromUnknownOrigin_ResponseMissingAllowOriginHeader` | Disallowed origin → no header on actual requests either. |
| `preflight_FromConfiguredOrigin_ResponseAllowsCredentials` | `Access-Control-Allow-Credentials: true` required for cookies in cross-origin requests. |
| `preflight_FromSwaggerOriginWhenNotConfigured_Returns403` | Only `febaseurl` set — Swagger origin not in allow-list → rejected. |

---

## `spring.oidc.bff` — Additional / Different Test Classes

### `oidcLogin()` instead of `@WithMockUser`
OIDC endpoints expect an `OidcUser` principal. `@WithMockUser` creates the wrong type (`UsernamePasswordAuthenticationToken`).

```java
.with(oidcLogin()
    .authorities(new SimpleGrantedAuthority("ROLE_myadmin"))
    .idToken(token -> token.claim("name", "Alice")))
```

No network call is made — no Okta tenant required.

---

### `HelloEndpointTest` (4 tests)
`GET /hello` — `permitAll()`. Controller: `return user != null ? user.getFullName() : null`.  
`getFullName()` reads the standard OIDC **`name`** claim from the ID token — not `given_name`/`family_name`.  
Tests set `.idToken(token -> token.claim("name", "..."))` and assert the response body, not just HTTP status.

> **Note:** `/hello` is a temporary diagnostic endpoint. It will be removed from both `spring.simple.bff` and `spring.oidc.bff` once no longer needed. These tests will be deleted along with it.

| Test | Why |
|---|---|
| `testHello_Unauthenticated_Returns200WithNullBody` | No principal → `getFullName()` not called → body is empty string. |
| `testHello_Authenticated_Returns200WithFullName` | `name` claim `"John Doe"` → body is `"John Doe"`. Confirms the controller reads the correct claim. |
| `testHello_AsMyuser_Returns200WithFullName` | Role does not affect `/hello` — full name is still returned regardless of authority. |
| `testHello_AsAdmin_Returns200WithFullName` | Same for admin role. |

---

### `ShortProfileEndpointTest` (6 tests)
Same endpoint but the JSON response includes a `scopes` field (separate from `roles`).

| Test | Why |
|---|---|
| `testShortProfile_WithScopeFoo_ReturnsProfileWithScope` | `SCOPE_foo` appears in `scopes`, not in `roles` — different JSON keys. |
| `testShortProfile_WithMultipleAuthorities_ReturnsCompleteProfile` | Both roles and scopes present simultaneously. |
| `testShortProfile_WithNoAuthorities_ReturnsBasicProfile` | Authenticated user with zero authorities → must not crash. |

---

### `SecuredFooEndpointTest` / `SecuredBarEndpointTest` (6 tests each)
Scope-based access using `hasAuthority("SCOPE_foo")` — **exact match**, not a role check.

| Test | Why |
|---|---|
| `testSecuredFoo_WithScopeFoo_Returns200` | Happy path. |
| `testSecuredFoo_WithScopeBar_Returns403` | Wrong scope — exact match enforced. |
| `testSecuredFoo_WithRoleMyuser_Returns403` | A role cannot satisfy a scope requirement. |
| `testSecuredFoo_WithMultipleScopes_Returns200` | Extra scope must not break access. |

---

### `ApiLogoutEndpointTest` (5 tests) — spring.oidc.bff
`GET /apilogout` — same endpoint as in `spring.simple.bff` but the OIDC variant also redirects to Okta's end-session endpoint when a genuine `OidcUser` with an ID token is present.

**Why `@WithMockUser` is used here (exception to the `oidcLogin()` rule):**

`/apilogout` is a custom controller method — not an OIDC flow. It accepts any caller regardless of principal type. The OIDC-specific redirect branch fires only when `oidcUser != null`. `@WithMockUser` produces a `UsernamePasswordAuthenticationToken`, so `@AuthenticationPrincipal OidcUser oidcUser` resolves to `null` — the endpoint clears the context and returns 200 without redirecting.

This is intentional: these tests verify that the endpoint handles non-OIDC callers cleanly (no crash, no redirect, correct 200). They do **not** test the Okta redirect branch.

**What is not covered here:** The redirect branch (302 to `{issuer}/v1/logout?id_token_hint=...`) requires a real `OidcUser` with a valid `idTokenValue`. That path is deferred to the real OIDC integration test suite.

| Test | Why |
|---|---|
| `testApiLogout_Unauthenticated_Returns200` | `permitAll` + GET → anonymous access; session clearance is a no-op. |
| `testApiLogout_AsUser_Returns200` | `@WithMockUser` — no `OidcUser`, no redirect; context cleared, returns 200. |
| `testApiLogout_AsAdmin_Returns200` | Same for admin. |
| `testApiLogout_WithSourceParameter_Returns200` | `source=frontend` accepted without error even when redirect branch does not fire. |
| `testApiLogout_WithSwaggerSource_Returns200` | `source=swagger` — the exact call the Swagger UI BFF plugin makes; returns 200, no redirect. |

---

### `SecurityConfigurationIntegrationTest` (7 tests)
**Cross-cutting access matrix.** Tests every authority type against every secured endpoint in one context.  
Motivation: Spring Security evaluates rules **top-to-bottom** — a misconfigured or wrong-order rule can shadow a later one.

| Test | Why |
|---|---|
| `testMyuserRole_AccessMatrix` | `ROLE_myuser` opens `/secured/user` only. |
| `testMyadminRole_AccessMatrix` | `ROLE_myadmin` opens `/secured/admin` only. |
| `testScopeFoo_AccessMatrix` | `SCOPE_foo` opens `/secured/foo` only — must not open role-gated endpoints. |
| `testScopeBar_AccessMatrix` | Same for `SCOPE_bar`. |
| `testMultipleRoles_AccessMatrix` | Both roles → both role-gated endpoints accessible simultaneously. |
| `testMultipleScopes_AccessMatrix` | Same for both scopes. |
| `testPowerUser_FullAccess` | All four authorities → every secured endpoint reachable. |

---

### `LogoutEndpointTest` — differences from `spring.simple.bff`
- Returns **204** No Content (not 200).
- `.deleteCookies("JSESSIONID")` is configured → test asserts `cookie().maxAge("JSESSIONID", 0)`.
- `logout_Authenticated_InvalidatesSession` uses `oidcLogin()` + `MockHttpSession` (not `httpBasic`).

---

### `AuthRedirectHandlerTest` (7 tests)
Tests the post-login redirect logic **directly as beans** (not through MockMvc end-to-end).

**Why beans, not MockMvc?** A full OAuth2 callback requires a live Okta tenant to produce a real authorization code. The redirect decision is pure Java logic — so it's tested by injecting the handler bean and calling `onAuthenticationSuccess()` / `onAuthenticationFailure()` directly.

**How the redirect works:**
1. SPA links to `/oauth2/authorization/okta?source=frontend`.
2. `CustomAuthorizationRequestResolver` saves `source` as a session attribute.
3. After Okta callback, `customSuccessHandler` reads `source` from session and redirects:
   - `"swagger"` → `swaggeruiurl`
   - `"frontend"` → `febaseurl`
   - anything else → `/`

| Test | Why |
|---|---|
| `successHandler_SourceSwagger_WithSwaggerUrl_RedirectsToSwaggerUrl` | Session `source=swagger` + `swaggeruiurl` configured → redirect to `http://localhost:5000`. |
| `successHandler_SourceFrontend_WithFeUrl_RedirectsToFeUrl` | Session `source=frontend` → redirect to `http://localhost:4200`. |
| `successHandler_NoSource_RedirectsToRoot` | No session attribute → fallback to `/`. |
| `successHandler_UnknownSource_RedirectsToRoot` | Unrecognised source value → fallback to `/`. |
| `failureHandler_SourceSwagger_RedirectsToSwaggerUrl` | On login failure, return to origin. |
| `failureHandler_NoSource_RedirectsToRoot` | Failure without source → `/`. |
| `failureHandler_SourceSwagger_SessionAttributeRemovedAfterUse` | Verifies the `source` session attribute is consumed (removed) by the failure handler after use, so it cannot influence a later request. |

---

## Quick Reference — Key Patterns

| Scenario | What to use |
|---|---|
| Testing authorization (who can reach what) | `@WithMockUser` (`simple.bff`) or `oidcLogin()` (`oidc.bff`) |
| Testing the actual login/auth stack | `httpBasic()` or form POST with real credentials |
| Any POST / PUT / DELETE | Must add `.with(csrf())` |
| Deliberately bad CSRF token | `.with(csrf().useInvalidToken())` |
| CORS tests | Set `febaseurl` property + add `Origin` header manually |
| Session invalidation across requests | Use real `MockHttpSession` across multiple `mockMvc.perform()` calls |
| Planned-but-not-built endpoints | Expect 401 (unauthenticated) and 404 (authenticated) |
| Redirect handler logic | Inject bean directly, call handler method, assert `response.getRedirectedUrl()` |

---

## Roles vs Scopes vs Authorities — Quick Reminder

| Concept | Authority string | Checked with |
|---|---|---|
| Role | `ROLE_myadmin` | `hasRole("myadmin")` — Spring prepends `ROLE_` automatically |
| Scope | `SCOPE_foo` | `hasAuthority("SCOPE_foo")` — exact match |
| Raw authority | any string | `hasAuthority("...")` — exact match |

`hasRole("myadmin")` ≡ `hasAuthority("ROLE_myadmin")`.  
`hasAuthority("SCOPE_foo")` ≠ `hasRole("SCOPE_foo")` (the latter would look for `ROLE_SCOPE_foo`).

---

## What Real (Non-Mock) Integration Tests Would Additionally Encounter

The current tests are **mock-based `@SpringBootTest` tests** — they load the full Spring context but never make real network calls. Here is what changes if you move to actual integration tests against a live environment.

---

### 1. `OktaOAuth2PropertiesMappingEnvironmentPostProcessor` — startup network call

The Okta Spring Boot starter registers an `EnvironmentPostProcessor` that fires **at Spring context startup** and makes a live HTTP call to the Okta discovery endpoint (`/.well-known/openid-configuration`) to resolve the issuer, token URI, JWKS URI, etc.

The test `application.properties` deliberately uses `spring.security.oauth2.client.*` properties directly and **omits `okta.oauth2.issuer`** specifically to prevent this processor from firing. A real integration test would need:
- A live Okta tenant reachable from the test runner
- Valid `client-id`, `client-secret`, `issuer`
- Network access (fails in CI/CD without proper secrets)

---

### 2. `customOidcUserService` — the `groups` claim mapping

Production code in `spring.oidc.bff`:
```java
Collection<String> groups = oidcUser.getAttribute("groups");
if (groups != null) {
    for (String group : groups) {
        mapped.add(new SimpleGrantedAuthority("ROLE_" + group));
    }
}
```

In mock tests, `oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))` **injects the authority directly** — it never goes through `customOidcUserService`.

A real integration test would need to verify:
- The Okta token actually carries a `groups` claim (Okta must be configured to include it)
- `customOidcUserService` correctly maps group names to `ROLE_*` authorities
- If Okta omits the `groups` claim (misconfiguration), `groups` is `null` and **no roles are assigned** — every `/secured/*` endpoint returns 403 — a bug the mock tests would never catch

---

### 3. `CustomAuthorizationRequestResolver` — the `source` session flow

`AuthRedirectHandlerTest` tests redirect handlers **by calling them as beans directly**. The full real flow is:
1. `GET /oauth2/authorization/okta?source=frontend` → resolver saves `source` to `HttpSession`
2. Browser redirects to Okta login page
3. Okta redirects back to `/login/oauth2/code/okta?code=...&state=...`
4. Spring exchanges the authorization code for an ID token (real HTTPS call to Okta)
5. `customOidcUserService` processes the token
6. `customSuccessHandler` reads `source` from session → redirects

Steps 2–5 are completely untested in the current suite. A real integration test would surface problems in state handling, `state` parameter mismatch, session timeouts between steps 1 and 6, and PKCE/nonce validation.

---

### 4. JWT signature verification (JWKS)

In production, Spring Security fetches the JWKS from `jwk-set-uri` to verify the ID token's signature on every login. Mock tests inject a pre-built `OidcUser` — no token is ever verified. Real integration tests would additionally surface:
- Clock skew between server and Okta (`iat`/`exp` claim failures)
- Wrong `aud` (audience) claim if `client-id` is misconfigured
- Key rotation — if Okta rotates its signing keys, the cached JWKS becomes stale

---

### 5. `BCryptPasswordEncoder` — where the current tests already reach the real stack

This is `spring.simple.bff` only. `FormLoginTest` and `HttpBasicAuthTest` **do** exercise the real `BCryptPasswordEncoder` by using actual credentials against `InMemoryUserDetailsManager`. This is the one area the current tests are **not** mock. `@WithMockUser`-based tests still bypass it.

---

### 6. Session / Cookie behaviour with a real browser

`MockMvc` runs in-process and simulates cookies as Java objects. A real browser (e.g. Selenium / Cypress) would additionally encounter:
- `SameSite` cookie attribute — if set to `Strict`, the `XSRF-TOKEN` cookie is not sent on cross-site navigations
- Cookie `Path` and `Domain` scope restrictions
- Browser preflight caching (`Access-Control-Max-Age`) — a successful preflight is cached; a subsequent config change may not take effect until the cache expires
- `Secure` flag — the cookie is not sent over plain HTTP; matters in local dev without HTTPS

---

### 7. Principal type — `@WithMockUser` vs `oidcLogin()`

Mock tests in `spring.simple.bff` use `@WithMockUser`, which injects a `UsernamePasswordAuthenticationToken` with a `User` principal. That is the correct type for form login / HTTP Basic.

Mock tests in `spring.oidc.bff` use `oidcLogin()`, which injects an `OAuth2AuthenticationToken` with an `OidcUser` principal. That is the correct type for OIDC.

**Why this matters in real integration tests:**  
If you accidentally used `@WithMockUser` in the OIDC project, the tests would compile and the security rules (roles, scopes) would appear to pass — but any code that touches the principal directly would throw a `ClassCastException` at runtime:

```java
@AuthenticationPrincipal OidcUser user   // ClassCastException if principal is User
```

Real integration tests against a live Okta tenant would surface this immediately because the actual authentication flow always produces an `OidcUser`. The mock tests only avoid the crash because `oidcLogin()` is used consistently — but the constraint is **not enforced by the compiler**, only by discipline.

| | Principal type | Token type |
|---|---|---|
| `@WithMockUser` | `org.springframework.security.core.userdetails.User` | `UsernamePasswordAuthenticationToken` |
| `oidcLogin()` | `org.springframework.security.oauth2.core.oidc.user.OidcUser` | `OAuth2AuthenticationToken` |

---

### Summary Table — Real Integration Test Coverage Gaps

| Scenario | What you would additionally test in real integration tests |
|---|---|
| Okta discovery / `issuer` resolution | Live HTTP call to Okta, checks for valid `issuer`, `token-uri`, `jwk-set-uri` |
| `customOidcUserService` mapping | Verifies `groups` claim from Okta, correct mapping to `ROLE_*` authorities |
| `source` session handling | Full OAuth2 flow from authorization code to token exchange, redirects |
| JWT signature verification | Real `jwk-set-uri` fetching, checks for clock skew, `aud` claim, key rotation |
| BCrypt (real in `FormLoginTest` / `HttpBasicAuthTest`) | Already covered for `spring.simple.bff` |
| Principal type mismatch (`@WithMockUser` vs `oidcLogin()`) | `ClassCastException` on `@AuthenticationPrincipal OidcUser` if wrong type injected |

---

## Test Pyramid — What to Test Where

> **Principle:** Mock tests own the detail. Real integration tests own the wiring.  
> A real integration test is **not** a repeat of the mock tests — it is a smoke test that the security foundation holds end-to-end against a live environment.

---

### `spring.simple.bff` — Real Integration Tests (5–7 smoke tests)

The security foundation here is: **BCrypt + InMemoryUserDetailsManager + form login + session cookie**.  
The mocks already cover all role/endpoint combinations in detail. The real integration tests only need to confirm the stack wires together correctly.

| Test | What it guards |
|---|---|
| App starts up and `/hello` returns 200 | Spring context loads, security filter chain initialises without errors |
| `POST /login` with valid credentials → 200 + session cookie issued | `BCryptPasswordEncoder` + `InMemoryUserDetailsManager` actually wire together |
| `POST /login` with wrong password → 401 | Real auth stack rejects bad credentials end-to-end |
| Authenticated session can reach `/secured/user` → 200 | Session cookie is valid; role from `InMemoryUserDetailsManager` is respected |
| `POST /logout` → 200 + session invalidated | Session is actually destroyed server-side; subsequent request with same session → 401 |
| `POST /checkpost` without CSRF token → 403 | CSRF filter is active in the real running app, not just in MockMvc |

> Everything else (role matrix, JSON shapes, CORS edge cases, invalid CSRF tokens, anonymous branches) is already covered comprehensively by the mock tests.

---

### `spring.oidc.bff` — Real Integration Tests (5–7 smoke tests)

The security foundation here is: **Okta OIDC + `customOidcUserService` + `groups` claim → `ROLE_*` mapping**.  
This is the layer the mocks **cannot reach** — `oidcLogin()` bypasses all of it.

| Test | What it guards |
|---|---|
| App starts up against live Okta without crashing | Okta discovery endpoint (`/.well-known/openid-configuration`) is reachable; `issuer`, `jwk-set-uri`, `token-uri` resolve correctly |
| Login flow completes and produces a valid session | Full OAuth2 code exchange works; `customOidcUserService` runs without error |
| Logged-in user in Okta group `myadmin` can reach `/secured/admin` → 200 | `groups` claim is present in token; `customOidcUserService` maps it to `ROLE_myadmin`; security rule fires correctly |
| Logged-in user in Okta group `myadmin` cannot reach `/secured/user` → 403 | Role mapping is exact; no accidental wildcard match |
| `POST /logout` → 204 + `JSESSIONID` cookie expired | Session destroyed; Okta back-channel logout not required but cookie must be cleared |
| Unauthenticated request to `/secured/admin` → 401 | `authenticationEntryPoint` fires correctly in a real running app |

> The `groups` claim test is the most critical one that mocks genuinely cannot cover — if Okta is misconfigured and omits the `groups` claim, `customOidcUserService` silently assigns no roles and every `/secured/*` endpoint returns 403 for everyone. Only a real integration test catches this.

---

### How to Run Real Integration Tests — Maven Profile

Real integration tests should be kept in `*IT.java` files and gated behind a Maven profile so they never run on a normal `mvn test`:

```xml
<profile>
    <id>integration-tests</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

```bash
# normal build — only mock tests (*Test.java) run via surefire
mvn test

# real integration tests (*IT.java) — requires live Okta credentials
mvn verify -P integration-tests \
  -Dokta.tenant.id=... \
  -Dokta.oauth2.client-id=... \
  -Dokta.oauth2.client-secret=...
```

This fits naturally alongside the existing `fullbuild` and `berun` profiles already in the project.