# Changelog

All notable changes to the Respectlytics Kotlin SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.0] - 2025-12-10

### ⚠️ Breaking Changes
- **REMOVED**: `identify(userId)` method - User identification no longer supported
- **REMOVED**: `reset()` method - No user state to reset
- **REMOVED**: `UserManager` class - No longer needed
- **REMOVED**: `user_id` field from Event - Session-based only
- **CHANGED**: `sessionTimeout` renamed to `sessionDuration` in Configuration
- **CHANGED**: Default session duration changed from 30 minutes to 2 hours

### Added
- RAM-only session storage - Session IDs never written to disk
- Automatic 2-hour session rotation
- New session generated on every app launch
- `forceNewSession()` internal method for testing
- `getTimeRemainingMs()` internal method for session inspection
- `resetForTesting()` method for test isolation

### Changed
- `SessionManager` now initializes session immediately at construction
- `SessionManager` no longer uses Storage for persistence
- Configuration parameter renamed: `sessionTimeout` → `sessionDuration`
- SDK log message now shows version: "v2.0.0 - Session-based analytics"

### Privacy Improvements
- No persistent identifiers stored on device
- Designed for GDPR/ePrivacy Directive compliance
- No consent banner required (no device storage)
- Cannot track users across app launches by design

### Migration Guide
Remove calls to `identify()` and `reset()` - they no longer exist:
```diff
  Respectlytics.configure(Configuration(apiKey = "your-api-key"))
- Respectlytics.identify("user-123")
  Respectlytics.track("purchase")
- Respectlytics.reset()
```

## [1.0.0] - 2025-12-06

### Added
- Initial release of Respectlytics Kotlin SDK
- Event tracking with `track(eventName, properties)`
- User identification with `identify(userId)` - auto-generates UUID if no ID provided
- Session management with 30-minute timeout rotation
- User management with persistent storage
- Event queue with immediate persistence to storage
- Automatic event batching (max 10 events or 30 seconds)
- Network client with retry logic and exponential backoff (max 3 attempts per call)
- Resource protection:
  - Max queue size enforcement (100 events, drops oldest)
  - Event TTL enforcement (7 days, auto-discard old events)
  - Max retry limits (10 attempts per event across flushes)
- Comprehensive configuration with 11 customizable parameters
- Thread-safe operations using synchronized methods
- Automatic metadata collection (timestamp, session_id, platform, os_version, app_version, locale, device_type)
- Privacy by design - no device identifiers, no IP logging, minimal data collection
- JVM and Android compatibility (API 24+)

### Dependencies
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `com.google.code.gson:gson:2.10.1`

[Unreleased]: https://github.com/respectlytics/respectlytics-kotlin/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/respectlytics/respectlytics-kotlin/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/respectlytics/respectlytics-kotlin/releases/tag/v1.0.0
