# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - 2025-12-19

### ⚠️ Breaking Changes
- **REMOVED**: `properties` parameter from `track()` method
- **REMOVED**: Collection of deprecated fields (`app_version`, `locale`)

### Changed
- `track(eventName: String)` now takes only the event name
- Event model now contains only 4 fields: `event_name`, `timestamp`, `session_id`, `platform`
- Updated README with ROA (Return of Avoidance) philosophy
- Removed GDPR/ePrivacy/CCPA compliance claims from documentation

### Why This Change?
The Respectlytics API enforces a strict allowlist of 4 stored fields to minimize data collection. Fields not on the allowlist were silently discarded, so removing them from the SDK reduces unnecessary data transmission and makes the SDK's behavior transparent.

### Migration
Remove any `properties` parameter from `track()` calls:

```kotlin
// Before
Respectlytics.track("purchase", mapOf("screen" to "checkout"))

// After
Respectlytics.track("purchase")
```

## [2.0.1] - 2025-12-13

### Changed
- Fixed version number in build.gradle.kts
- Updated privacy compliance wording in documentation

## [2.0.0] - 2025-12-10

### ⚠️ Breaking Changes
- **REMOVED**: `identify()` method
- **REMOVED**: `reset()` method

### Changed
- Session IDs now generated in RAM only (never persisted to disk)
- New session ID generated on every app launch
- Sessions rotate automatically every 2 hours (was 30 minutes inactivity)

### Migration
Remove any calls to `identify()` and `reset()`. Session management is now automatic.

## [1.0.0] - 2025-11-20

### Added
- Initial release
- Privacy-first analytics with session-based tracking
- Automatic session management
- Event batching and offline support
- Configurable timeouts and retry logic
