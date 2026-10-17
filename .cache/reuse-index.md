<!-- gerado por .ia/skills/reuse-index/scripts/build_index.py — não editar à mão -->
# Índice de reuso (gerado em 2026-10-17 04:18 UTC)

Índice heurístico (regex, não AST). Use para localizar candidatos rapidamente;
sempre abra o arquivo real antes de decidir reutilizar.

## backend (`backend`)

### src/main/java/com/eridani/achernar/identity/AchernarIdentityApplication.java
- `@EnableScheduling class AchernarIdentityApplication`
  - `main(...)`

### src/main/java/com/eridani/achernar/identity/account/Account.java
- ` record Account`
  - `active(...)`
  - `admin(...)`
  - `anonymized(...)`

### src/main/java/com/eridani/achernar/identity/account/AccountController.java
- `@RequestMapping class AccountController`
  - `me(...)`

### src/main/java/com/eridani/achernar/identity/account/AccountRepository.java
- `@Repository class AccountRepository`
  - `activate(...)`
  - `anonymize(...)`
  - `countActiveAdmins(...)`
  - `deletePending(...)`
  - `disable(...)`
  - `enable(...)`
  - `findByEmail(...)`
  - `findById(...)`
  - `findByIdForUpdate(...)`
  - `hasActiveAdmin(...)`
  - `insertAdmin(...)`
  - `insertIfAbsent(...)`
  - `one(...)`
  - `pendingOlderThan(...)`
  - `updateEmail(...)`
  - `updateLocale(...)`
  - `updateRole(...)`

### src/main/java/com/eridani/achernar/identity/account/AccountRole.java
- ` enum AccountRole`

### src/main/java/com/eridani/achernar/identity/account/AccountService.java
- `@Service class AccountService`
  - `view(...)`

### src/main/java/com/eridani/achernar/identity/account/AccountStatus.java
- ` enum AccountStatus`

### src/main/java/com/eridani/achernar/identity/account/EmailChangeController.java
- `@RestController class EmailChangeController`
- ` record ChangeRequest`
- ` record ConfirmRequest`
  - `confirm(...)`
  - `request(...)`

### src/main/java/com/eridani/achernar/identity/account/EmailChangeService.java
- `@Service class EmailChangeService`
  - `confirm(...)`
  - `request(...)`

### src/main/java/com/eridani/achernar/identity/account/OneTimeTokenService.java
- `@Service class OneTimeTokenService`
- ` record Issued`
- ` record Consumed`
  - `consume(...)`
  - `invalidate(...)`
  - `invalidateAll(...)`
  - `issue(...)`
  - `purge(...)`

### src/main/java/com/eridani/achernar/identity/account/RegistrationController.java
- `@RequestMapping class RegistrationController`
- ` record RegisterRequest`
- ` record VerifyRequest`
- ` record ResendRequest`
  - `neutral(...)`
  - `register(...)`
  - `resend(...)`
  - `verify(...)`

### src/main/java/com/eridani/achernar/identity/account/RegistrationService.java
- `@Service class RegistrationService`
  - `register(...)`
  - `resend(...)`
  - `sendVerification(...)`
  - `verifyEmail(...)`

### src/main/java/com/eridani/achernar/identity/account/TokenPurpose.java
- ` enum TokenPurpose`

### src/main/java/com/eridani/achernar/identity/admin/AccountAdminService.java
- `@Service class AccountAdminService`
  - `anonymize(...)`
  - `changeRole(...)`
  - `denied(...)`
  - `detail(...)`
  - `disable(...)`
  - `enable(...)`
  - `lastAdmin(...)`
  - `list(...)`

### src/main/java/com/eridani/achernar/identity/admin/AdminAccountController.java
- `@RequestMapping class AdminAccountController`
- ` record DisableRequest`
- ` record RoleRequest`
- ` record AnonymizeRequest`
  - `anonymize(...)`
  - `detail(...)`
  - `disable(...)`
  - `enable(...)`
  - `list(...)`
  - `role(...)`

### src/main/java/com/eridani/achernar/identity/admin/AdminAccountRepository.java
- `@Repository class AdminAccountRepository`
- ` record Row`
- ` record Filter`
- ` record EventRow`
  - `activeSessions(...)`
  - `find(...)`
  - `list(...)`
  - `recentEvents(...)`

### src/main/java/com/eridani/achernar/identity/admin/EventCursor.java
- ` record EventCursor`
  - `decode(...)`
  - `encode(...)`

### src/main/java/com/eridani/achernar/identity/admin/SecurityEventController.java
- `@RequestMapping class SecurityEventController`
  - `search(...)`

### src/main/java/com/eridani/achernar/identity/admin/SecurityEventQuery.java
- `@Service class SecurityEventQuery`
- ` record Criteria`
- ` record Row`
  - `search(...)`

### src/main/java/com/eridani/achernar/identity/audit/AuditEvent.java
- ` class AuditEvent`
  - `account(...)`
  - `actor(...)`
  - `client(...)`
  - `detail(...)`
  - `details(...)`
  - `of(...)`
  - `outcome(...)`
  - `session(...)`

### src/main/java/com/eridani/achernar/identity/audit/EventType.java
- ` enum EventType`
  - `allows(...)`
  - `defaultOutcome(...)`
  - `severity(...)`

### src/main/java/com/eridani/achernar/identity/audit/Outcome.java
- ` enum Outcome`

### src/main/java/com/eridani/achernar/identity/audit/SecurityEventRecorder.java
- `@Service class SecurityEventRecorder`
  - `anonymizeNetworkData(...)`
  - `persist(...)`
  - `purgeOlderThan(...)`
  - `record(...)`
  - `recordIndependent(...)`

### src/main/java/com/eridani/achernar/identity/audit/Severity.java
- ` enum Severity`

### src/main/java/com/eridani/achernar/identity/auth/AudiencePolicy.java
- `@Component class AudiencePolicy`
  - `forLogin(...)`
  - `forRefresh(...)`
  - `sanitize(...)`

### src/main/java/com/eridani/achernar/identity/auth/AudienceRules.java
- ` class AudienceRules`
  - `forLogin(...)`
  - `forRefresh(...)`
  - `sanitize(...)`

### src/main/java/com/eridani/achernar/identity/auth/AuthController.java
- `@RequestMapping class AuthController`
- ` record LoginRequest`
- ` record RefreshRequest`
  - `login(...)`
  - `logout(...)`
  - `refresh(...)`
  - `tokens(...)`

### src/main/java/com/eridani/achernar/identity/auth/LoginService.java
- `@Service class LoginService`
  - `failed(...)`
  - `locked(...)`
  - `login(...)`

### src/main/java/com/eridani/achernar/identity/auth/LogoutService.java
- `@Service class LogoutService`
  - `logout(...)`

### src/main/java/com/eridani/achernar/identity/auth/RefreshCookieFactory.java
- `@Component class RefreshCookieFactory`
  - `clear(...)`
  - `name(...)`
  - `read(...)`
  - `set(...)`

### src/main/java/com/eridani/achernar/identity/auth/RefreshDecision.java
- ` class RefreshDecision`
- ` enum Reuse`
  - `decide(...)`

### src/main/java/com/eridani/achernar/identity/auth/RefreshService.java
- `@Service class RefreshService`
- ` record Success`
- ` record Failure`
  - `refresh(...)`

### src/main/java/com/eridani/achernar/identity/auth/TokenResponses.java
- `@Component class TokenResponses`
- ` record Result`
  - `build(...)`

### src/main/java/com/eridani/achernar/identity/credential/CredentialConfig.java
- `@Configuration class CredentialConfig`

### src/main/java/com/eridani/achernar/identity/credential/PasswordController.java
- `@RestController class PasswordController`
- ` record ForgotRequest`
- ` record ResetRequest`
- ` record ReauthRequest`
- ` record ChangeRequest`
  - `change(...)`
  - `forgot(...)`
  - `reauth(...)`
  - `reset(...)`

### src/main/java/com/eridani/achernar/identity/credential/PasswordCredentialRepository.java
- `@Repository class PasswordCredentialRepository`
- ` record Credential`
  - `delete(...)`
  - `exists(...)`
  - `find(...)`
  - `rehash(...)`
  - `upsert(...)`

### src/main/java/com/eridani/achernar/identity/credential/PasswordHasher.java
- `@Component class PasswordHasher`
  - `hash(...)`
  - `needsRehash(...)`
  - `verify(...)`
  - `verifyDummy(...)`

### src/main/java/com/eridani/achernar/identity/credential/PasswordPolicy.java
- ` class PasswordPolicy`
  - `check(...)`
  - `maxLength(...)`
  - `minLength(...)`
  - `normalize(...)`
  - `withBundledList(...)`

### src/main/java/com/eridani/achernar/identity/credential/PasswordService.java
- `@Service class PasswordService`
  - `change(...)`
  - `forgot(...)`
  - `notifyChanged(...)`
  - `reauthenticate(...)`
  - `requireInput(...)`
  - `reset(...)`

### src/main/java/com/eridani/achernar/identity/mail/ActionLinks.java
- `@Component class ActionLinks`
  - `confirmEmailChange(...)`
  - `resetPassword(...)`
  - `uiForgotPassword(...)`
  - `uiSecurity(...)`
  - `uiSignIn(...)`
  - `verifyEmail(...)`

### src/main/java/com/eridani/achernar/identity/mail/MailBackoff.java
- ` class MailBackoff`
  - `nextDelay(...)`

### src/main/java/com/eridani/achernar/identity/mail/MailFailureClassifier.java
- ` class MailFailureClassifier`
  - `isPermanent(...)`
  - `safeError(...)`

### src/main/java/com/eridani/achernar/identity/mail/MailOutbox.java
- `@Service class MailOutbox`
- ` record Claimed`
  - `claim(...)`
  - `enqueue(...)`
  - `markFailed(...)`
  - `markRetry(...)`
  - `markSent(...)`
  - `pendingCount(...)`
  - `purge(...)`
  - `purgeForAccount(...)`

### src/main/java/com/eridani/achernar/identity/mail/MailRenderer.java
- `@Component class MailRenderer`
- ` record Rendered`
  - `addButton(...)`
  - `render(...)`

### src/main/java/com/eridani/achernar/identity/mail/MailService.java
- `@Service class MailService`
  - `humanDuration(...)`
  - `notice(...)`
  - `safeAddress(...)`
  - `send(...)`
  - `vars(...)`

### src/main/java/com/eridani/achernar/identity/mail/MailTemplate.java
- ` enum MailTemplate`
  - `fromId(...)`
  - `id(...)`

### src/main/java/com/eridani/achernar/identity/mail/OutboxWorker.java
- `@Component class OutboxWorker`
  - `handleFailure(...)`
  - `process(...)`
  - `runOnce(...)`
  - `tick(...)`

### src/main/java/com/eridani/achernar/identity/mail/SmtpMailSender.java
- `@Component class SmtpMailSender`

### src/main/java/com/eridani/achernar/identity/maintenance/RetentionService.java
- `@Service class RetentionService`
  - `run(...)`

### src/main/java/com/eridani/achernar/identity/maintenance/ScheduledJobs.java
- `@Component class ScheduledJobs`
  - `maintenance(...)`
  - `retention(...)`

### src/main/java/com/eridani/achernar/identity/ratelimit/LoginLockPolicy.java
- ` class LoginLockPolicy`
- ` record State`
- ` record Result`
  - `durationForLevel(...)`
  - `empty(...)`
  - `isLocked(...)`
  - `onFailure(...)`
  - `remainingSeconds(...)`

### src/main/java/com/eridani/achernar/identity/ratelimit/LoginLockService.java
- `@Service class LoginLockService`
- ` record Failure`
  - `purgeIdle(...)`
  - `registerFailure(...)`
  - `remainingLockSeconds(...)`
  - `reset(...)`

### src/main/java/com/eridani/achernar/identity/ratelimit/RateLimiter.java
- `@Service class RateLimiter`
- ` record Decision`
  - `check(...)`
  - `checkAll(...)`
  - `hit(...)`
  - `peek(...)`
  - `purgeOlderThan(...)`
  - `requireNotExceeded(...)`

### src/main/java/com/eridani/achernar/identity/ratelimit/RateRule.java
- ` enum RateRule`
  - `key(...)`
  - `limit(...)`
  - `prefix(...)`
  - `window(...)`

### src/main/java/com/eridani/achernar/identity/registry/AdminApiController.java
- `@RequestMapping class AdminApiController`
  - `create(...)`
  - `createCredential(...)`
  - `get(...)`
  - `list(...)`
  - `patch(...)`
  - `revokeCredential(...)`

### src/main/java/com/eridani/achernar/identity/registry/AdminApplicationController.java
- `@RequestMapping class AdminApplicationController`
  - `create(...)`
  - `createSecret(...)`
  - `get(...)`
  - `list(...)`
  - `patch(...)`
  - `revokeSecret(...)`

### src/main/java/com/eridani/achernar/identity/registry/Application.java
- ` record Application`
  - `active(...)`
  - `hasOrigin(...)`
  - `hasRedirectUri(...)`

### src/main/java/com/eridani/achernar/identity/registry/ApplicationInput.java
- ` record ApplicationInput`
- ` record AudienceInput`

### src/main/java/com/eridani/achernar/identity/registry/ApplicationPatch.java
- ` class ApplicationPatch`
  - `fromCreate(...)`
  - `merge(...)`
  - `version(...)`

### src/main/java/com/eridani/achernar/identity/registry/ApplicationRepository.java
- `@Repository class ApplicationRepository`
- ` record Summary`
- ` record SecretRow`
- ` record Base`
  - `activeSecretHashes(...)`
  - `count(...)`
  - `countActiveSecrets(...)`
  - `findByClientId(...)`
  - `findById(...)`
  - `insert(...)`
  - `insertSecret(...)`
  - `list(...)`
  - `listSecrets(...)`
  - `load(...)`
  - `originRegistered(...)`
  - `revokeSecret(...)`
  - `secretExists(...)`
  - `secretIsActive(...)`
  - `touchSecret(...)`
  - `update(...)`

### src/main/java/com/eridani/achernar/identity/registry/ApplicationService.java
- `@Service class ApplicationService`
- ` record Created`
- ` record SecretCreated`
  - `create(...)`
  - `createSecret(...)`
  - `findByClientId(...)`
  - `get(...)`
  - `list(...)`
  - `revokeSecret(...)`
  - `secrets(...)`
  - `update(...)`

### src/main/java/com/eridani/achernar/identity/registry/ApplicationValidator.java
- ` class ApplicationValidator`
- ` record Result`
  - `ok(...)`
  - `validate(...)`

### src/main/java/com/eridani/achernar/identity/registry/AudienceGrant.java
- ` record AudienceGrant`

### src/main/java/com/eridani/achernar/identity/registry/ClientAuthenticator.java
- `@Service class ClientAuthenticator`
  - `authenticate(...)`

### src/main/java/com/eridani/achernar/identity/registry/ClientContext.java
- ` record ClientContext`

### src/main/java/com/eridani/achernar/identity/registry/ClientType.java
- ` enum ClientType`

### src/main/java/com/eridani/achernar/identity/registry/DynamicCorsConfigurationSource.java
- `@Component class DynamicCorsConfigurationSource`
  - `getCorsConfiguration(...)`

### src/main/java/com/eridani/achernar/identity/registry/PublicConfigController.java
- `@RestController class PublicConfigController`
  - `config(...)`

### src/main/java/com/eridani/achernar/identity/registry/RegistryJson.java
- ` class RegistryJson`

### src/main/java/com/eridani/achernar/identity/registry/RegistryStatus.java
- ` enum RegistryStatus`

### src/main/java/com/eridani/achernar/identity/registry/ResourceApi.java
- ` record ResourceApi`
  - `active(...)`

### src/main/java/com/eridani/achernar/identity/registry/ResourceApiRepository.java
- `@Repository class ResourceApiRepository`
- ` record CredentialRow`
  - `activeCredentialHashes(...)`
  - `count(...)`
  - `countActiveCredentials(...)`
  - `credentialExists(...)`
  - `credentialIsActive(...)`
  - `findByAudience(...)`
  - `findByAudiences(...)`
  - `findById(...)`
  - `insert(...)`
  - `insertCredential(...)`
  - `list(...)`
  - `listCredentials(...)`
  - `one(...)`
  - `revokeCredential(...)`
  - `touchCredential(...)`
  - `update(...)`

### src/main/java/com/eridani/achernar/identity/registry/ResourceApiService.java
- `@Service class ResourceApiService`
- ` record Created`
- ` record CredentialCreated`
  - `authenticate(...)`
  - `create(...)`
  - `createCredential(...)`
  - `credentials(...)`
  - `get(...)`
  - `list(...)`
  - `revokeCredential(...)`
  - `update(...)`
  - `validateNameDescription(...)`

### src/main/java/com/eridani/achernar/identity/registry/UiApplicationSeeder.java
- `@Order class UiApplicationSeeder`
  - `run(...)`

### src/main/java/com/eridani/achernar/identity/registry/UrlRules.java
- ` class UrlRules`
  - `normalizeOrigin(...)`
  - `validateUrl(...)`

### src/main/java/com/eridani/achernar/identity/security/AccessPrincipal.java
- ` record AccessPrincipal`

### src/main/java/com/eridani/achernar/identity/security/AdminGuard.java
- `@Component class AdminGuard`
  - `require(...)`

### src/main/java/com/eridani/achernar/identity/security/BearerTokenFilter.java
- ` class BearerTokenFilter`
  - `reject(...)`

### src/main/java/com/eridani/achernar/identity/security/CurrentUser.java
- ` class CurrentUser`
  - `require(...)`

### src/main/java/com/eridani/achernar/identity/security/SecurityConfig.java
- `@Configuration class SecurityConfig`

### src/main/java/com/eridani/achernar/identity/session/RefreshTokenRepository.java
- `@Repository class RefreshTokenRepository`
- ` record Row`
  - `current(...)`
  - `currentIdOf(...)`
  - `findByHashForUpdate(...)`
  - `insert(...)`
  - `markRotated(...)`
  - `setReplacedBy(...)`

### src/main/java/com/eridani/achernar/identity/session/Session.java
- ` record Session`
  - `activeAt(...)`

### src/main/java/com/eridani/achernar/identity/session/SessionController.java
- `@RequestMapping class SessionController`
  - `endAll(...)`
  - `list(...)`
  - `revoke(...)`

### src/main/java/com/eridani/achernar/identity/session/SessionRepository.java
- `@Repository class SessionRepository`
- ` record View`
  - `audiences(...)`
  - `countActive(...)`
  - `find(...)`
  - `findForUpdate(...)`
  - `insert(...)`
  - `listActive(...)`
  - `markReauth(...)`
  - `oldestActive(...)`
  - `purge(...)`
  - `revoke(...)`
  - `revokeAll(...)`
  - `revokeAllForAccount(...)`
  - `revokeAllForApplication(...)`
  - `touch(...)`

### src/main/java/com/eridani/achernar/identity/session/SessionService.java
- `@Service class SessionService`
- ` record Created`
  - `create(...)`
  - `endAll(...)`
  - `isActive(...)`
  - `list(...)`
  - `markReauthenticated(...)`
  - `refreshExpiry(...)`
  - `require(...)`
  - `requireRecentReauth(...)`
  - `revoke(...)`
  - `revokeAllForAccount(...)`
  - `revokeAllForApplication(...)`
  - `revokeById(...)`
  - `revokeEverySession(...)`
  - `revokeOwn(...)`

### src/main/java/com/eridani/achernar/identity/setup/SetupController.java
- `@RequestMapping class SetupController`
- ` record CreateRequest`
  - `create(...)`
  - `status(...)`

### src/main/java/com/eridani/achernar/identity/setup/SetupService.java
- `@Service class SetupService`
- ` record Status`
- ` record Created`
  - `create(...)`
  - `status(...)`

### src/main/java/com/eridani/achernar/identity/setup/SetupStartupCheck.java
- `@Order class SetupStartupCheck`
  - `run(...)`

### src/main/java/com/eridani/achernar/identity/shared/config/AchernarProperties.java
- `@ConfigurationProperties record AchernarProperties`
- ` record Cookie`
- ` record Token`
- ` record Session`
- ` record Refresh`
- ` record Key`
- ` record Password`
- ` record OneTime`
- ` record Social`
- ` record Mail`
- ` record Retention`
- ` record Jobs`
  - `facebookConfigured(...)`
  - `googleConfigured(...)`
  - `issuerBase(...)`
  - `stripTrailingSlash(...)`
  - `uiBase(...)`

### src/main/java/com/eridani/achernar/identity/shared/config/CryptoConfig.java
- `@Configuration class CryptoConfig`

### src/main/java/com/eridani/achernar/identity/shared/config/PropertiesValidator.java
- `@Component class PropertiesValidator`
  - `validate(...)`

### src/main/java/com/eridani/achernar/identity/shared/config/SchedulingConfig.java
- `@EnableScheduling class SchedulingConfig`

### src/main/java/com/eridani/achernar/identity/shared/config/SecretsFromFiles.java
- ` class SecretsFromFiles`
  - `apply(...)`

### src/main/java/com/eridani/achernar/identity/shared/crypto/ConstantTime.java
- ` class ConstantTime`
  - `equals(...)`

### src/main/java/com/eridani/achernar/identity/shared/crypto/MasterKeyCipher.java
- ` class MasterKeyCipher`
  - `decrypt(...)`
  - `decryptString(...)`
  - `devInsecure(...)`
  - `encrypt(...)`
  - `encryptString(...)`
  - `fromBase64(...)`

### src/main/java/com/eridani/achernar/identity/shared/crypto/Pkce.java
- ` class Pkce`
  - `challengeFor(...)`
  - `matches(...)`
  - `validChallenge(...)`
  - `validVerifier(...)`

### src/main/java/com/eridani/achernar/identity/shared/crypto/Sha256.java
- ` class Sha256`
  - `digest(...)`
  - `hex(...)`

### src/main/java/com/eridani/achernar/identity/shared/crypto/TokenGenerator.java
- ` class TokenGenerator`
  - `newToken(...)`
  - `randomBytes(...)`

### src/main/java/com/eridani/achernar/identity/shared/db/Db.java
- ` class Db`
  - `advisoryXactLock(...)`
  - `escapeLike(...)`
  - `hash64(...)`
  - `tryAdvisoryXactLock(...)`
  - `ts(...)`

### src/main/java/com/eridani/achernar/identity/shared/db/JobLock.java
- `@Component class JobLock`
  - `runExclusive(...)`
  - `unlock(...)`

### src/main/java/com/eridani/achernar/identity/shared/db/PageResult.java
- ` record PageResult`
  - `clampPage(...)`
  - `clampSize(...)`
  - `toBody(...)`

### src/main/java/com/eridani/achernar/identity/shared/db/SortSpec.java
- ` class SortSpec`
  - `orderBy(...)`

### src/main/java/com/eridani/achernar/identity/shared/error/ApiException.java
- ` class ApiException`
  - `errorCode(...)`
  - `errors(...)`
  - `headers(...)`
  - `rateLimited(...)`
  - `retryAfterSeconds(...)`
  - `this(...)`
  - `validation(...)`
  - `withHeader(...)`

### src/main/java/com/eridani/achernar/identity/shared/error/ErrorCode.java
- ` enum ErrorCode`
  - `code(...)`
  - `name(...)`
  - `status(...)`
  - `typeSlug(...)`

### src/main/java/com/eridani/achernar/identity/shared/error/FieldError.java
- ` record FieldError`
  - `of(...)`

### src/main/java/com/eridani/achernar/identity/shared/error/ProblemDetailsAdvice.java
- `@RestControllerAdvice class ProblemDetailsAdvice`
  - `respond(...)`

### src/main/java/com/eridani/achernar/identity/shared/error/ProblemFactory.java
- `@Component class ProblemFactory`
  - `build(...)`
  - `toMap(...)`

### src/main/java/com/eridani/achernar/identity/shared/error/ProblemResponse.java
- `@JsonInclude record ProblemResponse`

### src/main/java/com/eridani/achernar/identity/shared/error/ProblemWriter.java
- `@Component class ProblemWriter`

### src/main/java/com/eridani/achernar/identity/shared/json/MiniJson.java
- ` class MiniJson`
- ` class Parser`
  - `error(...)`
  - `expect(...)`
  - `parseObject(...)`
  - `skipWs(...)`
  - `write(...)`
  - `writeString(...)`
  - `writeValue(...)`

### src/main/java/com/eridani/achernar/identity/shared/json/Obj.java
- ` class Obj`
  - `of(...)`
  - `put(...)`
  - `with(...)`
  - `withNullable(...)`

### src/main/java/com/eridani/achernar/identity/shared/util/EmailAddress.java
- ` class EmailAddress`
- ` record Normalized`
  - `domain(...)`
  - `localPart(...)`
  - `mask(...)`
  - `parse(...)`

### src/main/java/com/eridani/achernar/identity/shared/util/Time.java
- ` class Time`
  - `iso(...)`

### src/main/java/com/eridani/achernar/identity/shared/util/UuidV7.java
- ` class UuidV7`
  - `generate(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/BasicAuth.java
- ` class BasicAuth`
- ` record Credentials`
  - `parse(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/BodySizeLimitFilter.java
- ` class BodySizeLimitFilter`
- ` class LimitedRequest`
- ` class LimitedStream`
  - `isFinished(...)`
  - `isReady(...)`
  - `setReadListener(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/BodyTooLargeException.java
- ` class BodyTooLargeException`

### src/main/java/com/eridani/achernar/identity/shared/web/IpMasker.java
- ` class IpMasker`
  - `approximate(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/Ips.java
- ` class Ips`
  - `subject(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/LocaleSupport.java
- ` class LocaleSupport`
  - `isSupported(...)`
  - `resolve(...)`
  - `toJavaLocale(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/RequestIdFilter.java
- ` class RequestIdFilter`

### src/main/java/com/eridani/achernar/identity/shared/web/RequestInfo.java
- ` record RequestInfo`

### src/main/java/com/eridani/achernar/identity/shared/web/RequestInfoHolder.java
- ` class RequestInfoHolder`
  - `get(...)`
  - `runWith(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/SecurityHeadersFilter.java
- ` class SecurityHeadersFilter`

### src/main/java/com/eridani/achernar/identity/shared/web/TrustedProxyResolver.java
- ` class TrustedProxyResolver`
- ` record Cidr`
  - `isTrusted(...)`
  - `resolve(...)`

### src/main/java/com/eridani/achernar/identity/shared/web/UserAgentSummarizer.java
- ` class UserAgentSummarizer`
  - `summarize(...)`

### src/main/java/com/eridani/achernar/identity/social/FacebookClient.java
- `@Component class FacebookClient`
  - `append(...)`
  - `authorizationUrl(...)`
  - `configured(...)`
  - `fetchIdentity(...)`
  - `provider(...)`

### src/main/java/com/eridani/achernar/identity/social/GoogleClient.java
- `@Component class GoogleClient`
  - `append(...)`
  - `authorizationUrl(...)`
  - `configured(...)`
  - `fetchIdentity(...)`
  - `provider(...)`
  - `validate(...)`

### src/main/java/com/eridani/achernar/identity/social/Provider.java
- ` enum Provider`
  - `amr(...)`
  - `fromSlug(...)`
  - `slug(...)`

### src/main/java/com/eridani/achernar/identity/social/ProviderClient.java
- ` interface ProviderClient`
  - `authorizationUrl(...)`
  - `configured(...)`
  - `fetchIdentity(...)`
  - `provider(...)`

### src/main/java/com/eridani/achernar/identity/social/ProviderException.java
- ` class ProviderException`
  - `invalid(...)`
  - `unavailable(...)`

### src/main/java/com/eridani/achernar/identity/social/ProviderHttp.java
- `@Component class ProviderHttp`
  - `getJson(...)`
  - `postForm(...)`

### src/main/java/com/eridani/achernar/identity/social/ProviderIdentity.java
- ` record ProviderIdentity`

### src/main/java/com/eridani/achernar/identity/social/SocialAccountDecision.java
- ` class SocialAccountDecision`
- ` enum Outcome`
  - `decide(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialController.java
- `@RequestMapping class SocialController`
- ` record ExchangeRequest`
  - `callback(...)`
  - `exchange(...)`
  - `redirect(...)`
  - `start(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialExchangeCodeRepository.java
- `@Repository class SocialExchangeCodeRepository`
- ` record Code`
  - `findByHashForUpdate(...)`
  - `insert(...)`
  - `markConsumed(...)`
  - `purgeOlderThan(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialIntentCodec.java
- `@Component class SocialIntentCodec`
- ` record Intent`
  - `decode(...)`
  - `encode(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialLink.java
- ` record SocialLink`

### src/main/java/com/eridani/achernar/identity/social/SocialLinkController.java
- `@RestController class SocialLinkController`
- ` record IntentRequest`
  - `linkIntent(...)`
  - `reauthIntent(...)`
  - `unlink(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialLinkRepository.java
- `@Repository class SocialLinkRepository`
  - `count(...)`
  - `delete(...)`
  - `deleteAll(...)`
  - `findByAccount(...)`
  - `findByAccountAndProvider(...)`
  - `findByIdentity(...)`
  - `insert(...)`
  - `touchLogin(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialLinkService.java
- `@Service class SocialLinkService`
  - `intent(...)`
  - `unlink(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialLoginService.java
- `@Service class SocialLoginService`
- ` record StartParams`
- ` record Redirect`
- ` record ExchangeOk`
- ` record ExchangeFail`
- ` record Outcome`
  - `bindingCookieName(...)`
  - `callback(...)`
  - `callbackUrl(...)`
  - `createActive(...)`
  - `exchange(...)`
  - `failEvent(...)`
  - `finish(...)`
  - `providerEnabled(...)`
  - `providerName(...)`
  - `start(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialPageException.java
- ` class SocialPageException`
  - `status(...)`

### src/main/java/com/eridani/achernar/identity/social/SocialTransaction.java
- ` record SocialTransaction`
- ` enum Purpose`

### src/main/java/com/eridani/achernar/identity/social/SocialTransactionRepository.java
- `@Repository class SocialTransactionRepository`
- ` enum ConsumeStatus`
- ` record Consumed`
  - `consume(...)`
  - `findById(...)`
  - `insert(...)`
  - `purgeOlderThan(...)`

### src/main/java/com/eridani/achernar/identity/token/AccessTokenVerifier.java
- `@Component class AccessTokenVerifier`
- ` record Verified`
  - `clockNow(...)`
  - `verify(...)`

### src/main/java/com/eridani/achernar/identity/token/AdminKeyController.java
- `@RequestMapping class AdminKeyController`
- ` record RemoveRequest`
  - `list(...)`
  - `remove(...)`
  - `rotate(...)`

### src/main/java/com/eridani/achernar/identity/token/IntrospectionController.java
- `@RestController class IntrospectionController`

### src/main/java/com/eridani/achernar/identity/token/IntrospectionService.java
- `@Service class IntrospectionService`
  - `inactive(...)`
  - `introspect(...)`

### src/main/java/com/eridani/achernar/identity/token/InvalidTokenException.java
- ` class InvalidTokenException`

### src/main/java/com/eridani/achernar/identity/token/JwksController.java
- `@RestController class JwksController`
  - `jwks(...)`
  - `metadata(...)`

### src/main/java/com/eridani/achernar/identity/token/JwtIssuer.java
- `@Component class JwtIssuer`
- ` record Issued`
- ` record Request`
  - `issue(...)`

### src/main/java/com/eridani/achernar/identity/token/KeyRotationJob.java
- `@Order class KeyRotationJob`
  - `rotate(...)`
  - `run(...)`

### src/main/java/com/eridani/achernar/identity/token/KeyRotationPlanner.java
- ` class KeyRotationPlanner`
- ` enum Type`
- ` record Action`
- ` record KeyState`
- ` record Params`
  - `plan(...)`

### src/main/java/com/eridani/achernar/identity/token/KeyStatus.java
- ` enum KeyStatus`

### src/main/java/com/eridani/achernar/identity/token/SigningKey.java
- ` record SigningKey`

### src/main/java/com/eridani/achernar/identity/token/SigningKeyHealthIndicator.java
- `@Component class SigningKeyHealthIndicator`
  - `health(...)`

### src/main/java/com/eridani/achernar/identity/token/SigningKeyRepository.java
- `@Repository class SigningKeyRepository`
  - `activate(...)`
  - `findActive(...)`
  - `findAll(...)`
  - `findByKid(...)`
  - `findPublishable(...)`
  - `insert(...)`
  - `purgeRemoved(...)`
  - `remove(...)`
  - `retire(...)`

### src/main/java/com/eridani/achernar/identity/token/SigningKeyService.java
- `@Service class SigningKeyService`
- ` record Signer`
- ` record RunResult`
- ` record CachedSigner`
- ` record CachedVerification`
  - `activeKeyActivatedAt(...)`
  - `activeSigner(...)`
  - `ensureActiveKey(...)`
  - `hasActiveKey(...)`
  - `invalidateCaches(...)`
  - `jwksJson(...)`
  - `list(...)`
  - `purgeRemoved(...)`
  - `remove(...)`
  - `rotateNow(...)`
  - `runScheduled(...)`
  - `verificationKeys(...)`

### src/test/java/com/eridani/achernar/identity/it/AbstractIT.java
- `@ActiveProfiles class AbstractIT`
- ` record Resp`
  - `all(...)`
  - `collect(...)`
  - `get(...)`
  - `header(...)`
  - `json(...)`
  - `post(...)`
  - `send(...)`
  - `sleep(...)`
  - `str(...)`

### src/test/java/com/eridani/achernar/identity/it/AdminProtectionIT.java
- ` class AdminProtectionIT`

### src/test/java/com/eridani/achernar/identity/it/AuthFlowIT.java
- ` class AuthFlowIT`
  - `ensureAdmin(...)`

### src/test/java/com/eridani/achernar/identity/it/HealthAndKeysIT.java
- ` class HealthAndKeysIT`

### src/test/java/com/eridani/achernar/identity/it/RefreshConcurrencyIT.java
- ` class RefreshConcurrencyIT`

## frontend (`frontend`)

### src/app/app.ts
- ` class App`

### src/app/core/auth/api.interceptor.ts
- ` component apiInterceptor`

### src/app/core/auth/auth.models.ts
- ` interface TokenResponse`
- ` interface SocialLink`
- ` interface AccountView`

### src/app/core/auth/guards.ts
- `@Injectable class SetupStatusService`
- ` component authGuard`
- ` component adminGuard`
  - `markDone(...)`

### src/app/core/auth/reauth-dialog/reauth-dialog.component.ts
- ` class ReauthDialogComponent`
  - `cancel(...)`

### src/app/core/auth/reauth.service.ts
- `@Injectable class ReauthService`
  - `request(...)`

### src/app/core/auth/refresh-coordinator.ts
- `@Injectable class RefreshCoordinator`

### src/app/core/auth/session.service.ts
- `@Injectable class SessionService`
- ` function isRetryable`
  - `clearLocal(...)`
  - `expire(...)`
  - `refresh(...)`

### src/app/core/auth/social-flow.service.ts
- ` interface PendingSocial`
- `@Injectable class SocialFlowService`
  - `consume(...)`
  - `safeRemove(...)`

### src/app/core/auth/token-store.ts
- `@Injectable class TokenStore`
  - `clear(...)`
  - `expiresSoon(...)`
  - `set(...)`

### src/app/core/config/runtime-config.ts
- ` interface RuntimeConfig`
- `@Injectable class RuntimeConfigService`
  - `url(...)`

### src/app/core/http/api-error.ts
- ` class ApiError`
- ` function toApiError`
  - `constructor(...)`
  - `fieldCode(...)`

### src/app/core/i18n/transloco-loader.ts
- `@Injectable class TranslocoHttpLoader`
  - `getTranslation(...)`

### src/app/core/models/api.types.ts
- ` interface Page`
- ` interface FieldIssue`
- ` interface ApiProblem`

### src/app/core/services/connectivity.service.ts
- `@Injectable class ConnectivityService`
  - `constructor(...)`
  - `markAvailable(...)`
  - `markUnavailable(...)`

### src/app/core/services/error-message.service.ts
- `@Injectable class ErrorMessageService`
  - `field(...)`
  - `message(...)`

### src/app/core/services/loading.service.ts
- `@Injectable class LoadingService`
  - `begin(...)`
  - `clearTimeout(...)`
  - `end(...)`

### src/app/core/services/locale.service.ts
- ` function detectLang`
- `@Injectable class LocaleService`
  - `init(...)`
  - `safeSet(...)`
  - `set(...)`

### src/app/core/services/notify.service.ts
- `@Injectable class NotifyService`
  - `error(...)`
  - `failure(...)`
  - `info(...)`
  - `success(...)`

### src/app/core/services/theme.service.ts
- `@Injectable class ThemeService`
  - `constructor(...)`
  - `safeSet(...)`
  - `set(...)`

### src/app/core/utils/countdown.ts
- ` function createCountdown`
  - `clearInterval(...)`
  - `stop(...)`

### src/app/core/utils/fragment.ts
- ` function takeFragmentToken`

### src/app/core/utils/pkce.ts
- ` function randomToken`

### src/app/core/utils/safe-redirect.ts
- ` function safeNext`

### src/app/core/utils/safe-storage.ts
- ` function safeGet`
- ` function safeSet`
- ` function safeRemove`

### src/app/core/utils/validators.ts
- ` function passwordLengthValidator`
- ` function matchValidator`
- ` function isValidOrigin`
- ` function isValidAbsoluteUrl`
- ` function isValidAudience`
- ` function passwordStrength`

### src/app/features/account/components/email-section.component.ts
- ` class EmailSectionComponent`
  - `constructor(...)`
  - `submit(...)`

### src/app/features/account/components/password-section.component.ts
- ` interface PasswordSubmit`
- ` class PasswordSectionComponent`
  - `constructor(...)`
  - `reset(...)`
  - `submit(...)`

### src/app/features/account/components/social-links-section.component.ts
- ` class SocialLinksSectionComponent`

### src/app/features/account/models/account.models.ts
- ` interface SessionItem`
- ` interface ChangePasswordBody`

### src/app/features/account/pages/account.page.ts
- ` class AccountPage`
  - `ngOnInit(...)`

### src/app/features/account/pages/security.page.ts
- ` class SecurityPage`

### src/app/features/account/pages/sessions.page.ts
- ` class SessionsPage`
  - `ngOnInit(...)`

### src/app/features/account/services/account.service.ts
- `@Injectable class AccountService`
  - `changePassword(...)`
  - `endAll(...)`
  - `requestEmailChange(...)`
  - `revokeSession(...)`
  - `sessions(...)`
  - `unlinkSocial(...)`

### src/app/features/admin/components/admin-tones.ts
- ` function accountStatusTone`
- ` function registryStatusTone`
- ` function severityTone`
- ` function outcomeTone`

### src/app/features/admin/components/application-fields.component.ts
- ` class ApplicationFieldsComponent`
  - `choose(...)`
  - `err(...)`
  - `groupOf(...)`

### src/app/features/admin/components/application-form.ts
- ` function buildApplicationForm`
- ` function addAudience`
- ` function patchApplicationForm`
- ` function toPayload`
  - `addAudience(...)`

### src/app/features/admin/components/secrets-section.component.ts
- ` class SecretsSectionComponent`

### src/app/features/admin/models/admin.models.ts
- ` interface AdminAccountSummary`
- ` interface EventBrief`
- ` interface AdminAccountDetail`
- ` interface AccountFilters`
- ` interface ApplicationSummary`
- ` interface AudienceEntry`
- ` interface SecretRow`
- ` interface ApplicationDetail`
- ` interface ApplicationPayload`
- ` interface ApiSummary`
- ` interface SigningKey`
- ` interface SecurityEventItem`
- ` interface EventFilters`

### src/app/features/admin/pages/account-detail.page.ts
- ` class AccountDetailPage`
  - `ngOnInit(...)`

### src/app/features/admin/pages/accounts-list.page.ts
- ` class AccountsListPage`
  - `apply(...)`
  - `clear(...)`
  - `ngOnInit(...)`
  - `open(...)`
  - `paged(...)`

### src/app/features/admin/pages/api-form.page.ts
- ` class ApiFormPage`

### src/app/features/admin/pages/apis-list.page.ts
- ` class ApisListPage`
  - `apply(...)`
  - `ngOnInit(...)`
  - `paged(...)`

### src/app/features/admin/pages/application-form.page.ts
- ` class ApplicationFormPage`
  - `addApi(...)`
  - `addAudience(...)`
  - `patchApplicationForm(...)`
  - `removeApi(...)`

### src/app/features/admin/pages/applications-list.page.ts
- ` class ApplicationsListPage`
  - `apply(...)`
  - `ngOnInit(...)`
  - `paged(...)`

### src/app/features/admin/pages/events.page.ts
- ` class EventsPage`
  - `clear(...)`
  - `detailsOf(...)`
  - `isOpen(...)`
  - `ngOnInit(...)`
  - `toggle(...)`
  - `tone(...)`

### src/app/features/admin/pages/keys.page.ts
- ` class KeysPage`
  - `canRemove(...)`
  - `ngOnInit(...)`
  - `tone(...)`

### src/app/features/admin/services/admin-accounts.service.ts
- `@Injectable class AdminAccountsService`
  - `anonymize(...)`
  - `changeRole(...)`
  - `disable(...)`
  - `enable(...)`
  - `get(...)`
  - `list(...)`

### src/app/features/admin/services/admin-apis.service.ts
- ` interface ApiCreated`
- `@Injectable class AdminApisService`
  - `createCredential(...)`
  - `get(...)`
  - `list(...)`
  - `revokeCredential(...)`

### src/app/features/admin/services/admin-applications.service.ts
- `@Injectable class AdminApplicationsService`
  - `create(...)`
  - `createSecret(...)`
  - `get(...)`
  - `list(...)`
  - `revokeSecret(...)`

### src/app/features/admin/services/admin-events.service.ts
- ` interface EventsPage`
- `@Injectable class AdminEventsService`
  - `list(...)`

### src/app/features/admin/services/admin-keys.service.ts
- `@Injectable class AdminKeysService`
  - `list(...)`
  - `rotate(...)`

### src/app/features/auth/components/social-buttons.component.ts
- ` class SocialButtonsComponent`
  - `choose(...)`

### src/app/features/auth/models/auth.models.ts
- ` interface PublicConfig`

### src/app/features/auth/pages/check-email.page.ts
- ` class CheckEmailPage`

### src/app/features/auth/pages/confirm-email-change.page.ts
- ` class ConfirmEmailChangePage`

### src/app/features/auth/pages/email-only.page.ts
- ` class EmailOnlyPage`

### src/app/features/auth/pages/login.page.ts
- ` class LoginPage`

### src/app/features/auth/pages/register.page.ts
- ` class RegisterPage`

### src/app/features/auth/pages/reset-password.page.ts
- ` class ResetPasswordPage`
  - `ngOnInit(...)`

### src/app/features/auth/pages/social-callback.page.ts
- ` function socialErrorKey`
- ` class SocialCallbackPage`

### src/app/features/auth/pages/verify-email.page.ts
- ` class VerifyEmailPage`

### src/app/features/auth/services/auth-api.service.ts
- `@Injectable class AuthApiService`
  - `config(...)`
  - `confirmEmailChange(...)`
  - `exchangeSocial(...)`
  - `forgotPassword(...)`
  - `register(...)`
  - `resendVerification(...)`
  - `resetPassword(...)`
  - `verifyEmail(...)`

### src/app/features/errors/pages/error.page.ts
- ` class ErrorPage`

### src/app/features/setup/pages/setup.page.ts
- ` class SetupPage`
  - `applyServerErrors(...)`
  - `signIn(...)`

### src/app/features/setup/services/setup.service.ts
- ` interface SetupAdminRequest`
- `@Injectable class SetupService`
  - `create(...)`

### src/app/layout/navigation/nav-items.ts
- ` interface NavItem`

### src/app/layout/navigation/preferences-menu.component.ts
- ` class PreferencesMenuComponent`
  - `themeIcon(...)`

### src/app/layout/shell/shell.component.ts
- ` class ShellComponent`
  - `constructor(...)`
  - `skipToContent(...)`

### src/app/shared/components/auth-layout/auth-layout.component.ts
- ` class AuthLayoutComponent`

### src/app/shared/components/brand/brand.component.ts
- ` class BrandComponent`

### src/app/shared/components/chips-input/chips-input.component.ts
- ` class ChipsInputComponent`
  - `add(...)`
  - `constructor(...)`
  - `remove(...)`

### src/app/shared/components/confirm-dialog/confirm-dialog.component.ts
- ` interface ConfirmData`
- ` interface ConfirmResult`
- ` class ConfirmDialogComponent`
  - `canConfirm(...)`
  - `cancel(...)`
  - `confirm(...)`

### src/app/shared/components/empty-state/empty-state.component.ts
- ` class EmptyStateComponent`

### src/app/shared/components/error-summary/error-summary.component.ts
- ` interface SummaryItem`
- ` class ErrorSummaryComponent`
  - `constructor(...)`
  - `go(...)`

### src/app/shared/components/form-alert/form-alert.component.ts
- ` class FormAlertComponent`

### src/app/shared/components/page-header/page-header.component.ts
- ` class PageHeaderComponent`

### src/app/shared/components/password-field/password-field.component.ts
- ` class PasswordFieldComponent`
  - `error(...)`
  - `onInput(...)`
  - `toggle(...)`

### src/app/shared/components/secret-reveal-dialog/secret-reveal-dialog.component.ts
- ` interface SecretRevealData`
- ` class SecretRevealDialogComponent`
  - `close(...)`
  - `constructor(...)`

### src/app/shared/components/skeleton/skeleton.component.ts
- ` class SkeletonComponent`
  - `rows(...)`

### src/app/shared/components/status-chip/status-chip.component.ts
- ` class StatusChipComponent`

### src/app/shared/components/text-field/text-field.component.ts
- ` class TextFieldComponent`
  - `error(...)`

### src/app/shared/directives/focus-invalid.directive.ts
- ` class FocusInvalidDirective`
  - `focusFirstInvalid(...)`

### src/app/shared/pipes/local-date.pipe.ts
- `@Pipe class LocalDatePipe`
  - `transform(...)`

### src/app/shared/utils/form-errors.ts
- ` interface ErrorText`
- ` function firstError`

### src/app/shared/utils/server-errors.ts
- ` function applyServerErrors`

---

250 arquivo(s) indexado(s).