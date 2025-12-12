# Changelog

All notable changes to the Respectlytics Kotlin SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.1] - 2025-12-12

### Changed
- Updated privacy compliance wording in documentation to clarify regulatory requirements and recommend legal consultation

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

### Why This Change?
Storing identifiers on device requires user consent under ePrivacy Directive Article 5(3).
In-memory sessions require no consent, making Respectlytics designed for consent-free analytics.

### Migration
Remove any calls to `identify()` and `reset()`. Session management is now automatic.

## [1.0.1] - 2025-11-30

### Fixed
- Minor bug fixes and stability improvements

## [1.0.0] - 2025-11-15

### Added
- Initial release
- Privacy-first analytics with session-based tracking
- Automatic session management
- Event batching and offline support
