# Changelog

<!---
https://keepachangelog.com/
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
--->

## [v1.8.0] - 2023-05-XX

### Added

- SettingsActivity: SINETStream: [SAME as above]
- SettingsActivity: Sensor: Add icons for some items.

### Changed

- MainActivity: Check runtime permissions in collective way by using `PermissionHandler` in libhelper.
- MainActivity: If predefined parameters exist, read them without user interventions.
- Update build environ


## [v1.6.1] - 2022-12-12

### Added

- Add option to download SINETStream parameters from specified Config Server.
- Add support for monitoring cellular network signal strengths
- Add Select all sensor types button
- Run on Android 12+ devices, along with PahoMqttAndroid-bugfix library

### Changed

- Reorganize Settings layer structures
- Follow guidelines for runtime permissions handling
- Add timestamp for the device section items (location, cellular) in the output JSON data
- Show progress bar if the broker connection has lost and auto-reconnect procedure is running

### Removed

- Embedded SINETStream configuration file has removed.

### Fixed

- Fix some lint warnings, such as rewrite obsoleted API usages

### Security

- Update Android Studio and its tools
- Update all dependency modules


## [v1.6.0] - 2021-12-22

### Added

- SettingsActivity: SINETStream: Add detailed parameters for MQTT and SSL/TLS.
- SettingsActivity: Sensor: Add `automatic location update` mode with GPS or FLP.
- MainActivity: Add location tracker and foreground services for GPS and FLP.
- MainActivity: Show location monitor window if location is enabled.

### Changed

- build.gradle: Use MavenCentral instead of jCenter
- build.gradle: Use JDK 11 instead of JDK 8, from Android Studio Arctic Fox.

- SettingsActivity: Rearrange menu hierarchy.
- MainActivity: For SSL/TLS connection, operation will be intercepted by a system dialog to pick up credentials.
- MainActivity: For `automatic location update` mode, operation might be intercepted by several system dialogs to set appropriate permissions.

### Removed

- SettingsActivity: Exclude TLSv1 and TLSv1.1 from menu items, and set TLSv1.2 as default.

### Fixed

- MainActivity: Keep some attributes beyond Activity`s lifecycle.
- MainActivity: Fix location notation: (longitude,latitude) -> (latitude,longitude)
- MainActivity: Resolve race conditions between Sensor and Network; bind SensorService after connection has established, and unbind SensorService after connection has closed.


## [v1.5.3] - 2021-05-20

### Changed

- build.gradle: Update build environ for the Android Studio 4.1.2.

### Fixed

- MainActivity: Resolve race conditions between modal dialogs.


## [v1.5.0] - 2021-03-18

### Added

- Initial release
