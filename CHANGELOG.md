# Changelog

All notable changes to the Respectlytics Kotlin SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
- 90 comprehensive unit tests
- 6 integration tests against Django API
- Security verification (no hardcoded API keys or local paths)
- Complete API documentation with KDoc comments

### Dependencies
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `com.google.code.gson:gson:2.10.1`

### Testing
- ConfigurationTest - 15 tests
- StorageTest - 10 tests
- SessionManagerTest - 10 tests
- UserManagerTest - 13 tests
- EventTest - 9 tests
- EventQueueTest - 12 tests
- RespectlyticsTest - 21 tests
- IntegrationTest - 6 tests
- **Total: 96 tests passing**

[Unreleased]: https://github.com/respectlytics/respectlytics-kotlin/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/respectlytics/respectlytics-kotlin/releases/tag/v1.0.0
