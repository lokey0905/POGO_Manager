# Detailed usage and compatibility

[繁體中文](app-guide.md) | English · [Project overview](../README.en.md)

This page preserves the detailed workflows, integration differences, and download limitations moved from the README. The source tree may differ from published APKs.

## Features

### App versions and downloads

- Detect installed versions of configured apps and compare them with remote version information where a source is available.
- Select a target Pokémon GO or Monster Hunter Now version and view companion-tool compatibility, including unknown states and the highest supported version when available. Full support lists can be expanded on demand.
- Check stable and test channels for PGTools and MHNTools, with a switch controlling the download channel.
- Open download pages, installed apps, app settings, and system uninstall prompts. Some entries are download links only; an entry in the catalog does not imply automatic version or compatibility detection.
- Refresh version information without treating an app's latest release as proof that it supports the selected game version.

### Device checks and utilities

- Display device and Android details, check location/network-provider settings and mock-location flags, and report root/Magisk indicators.
- Launch external integrity and environment checkers after a confirmation dialog. Their results are informational, not a guarantee of game compatibility or device security.
- Provide shortcuts to system settings, game app settings, guides, a web-based cooldown calculator, and GPX/download resources. Supported entries can be pinned to the launcher; a location-accuracy settings widget is also available.
- Use Material-themed screens with dark-mode support, dynamic colors where available, and responsive layouts, including a left navigation rail on sufficiently wide landscape tablets.

## Supported apps and games

“Supported” here means that POGO Manager provides an integration or catalog entry, not that a third-party app is endorsed or compatible with every device or game release.

| Game / workflow | Included integrations | Compatibility information |
| --- | --- | --- |
| Pokémon GO | Standard and Samsung variants; PGTools, Pokemod, PGSharp Root, PolygonX, Data Hub, GPS JoyStick, DeFit, PokeList, WeCatch, Galaxy Store, and APKMirror Installer | Target-game compatibility for PGTools stable/test, Pokemod, and PGSharp Root. PolygonX checks its own app version, not a remote Pokémon GO support list. |
| Monster Hunter Now | Game downloads, MHNTools, Hylianer, GPS JoyStick, and APKMirror Installer | Target-game compatibility for MHNTools stable/test. Hylianer has a download/open entry, not an automatic latest-version check. |
| Pikmin Bloom | Pikmin Bloom, GPS JoyStick, DeFit, Fitness Hooks, Google Fit, Health Connect, and APKMirror Installer | App-version and download catalog, not a Pikmin compatibility matrix. Google Fit and Health Connect use store/system actions rather than remote version checks. |

Pikmin downloads open APKMirror; Google Fit and Health Connect download links open Google Play. On applicable Android versions, Health Connect opens as a system component. GPS JoyStick detection uses the package names configured by this project, so differently packaged installations may not be recognized.

## Requirements and compatibility

- **Android 9 / API 28 or newer** is the app's minimum requirement. Companion apps have their own Android, architecture, root, or framework requirements.
- Version checks and download links require an internet connection. Availability depends on upstream APIs, websites, stores, and metadata; missing or stale data is not proof of compatibility.
- Location tests require location permission and enabled location services. Installed-app visibility is used for package/version detection. Launcher support determines whether shortcuts and widgets can be pinned.
- Most game/tool download actions are restricted to `arm64-v8a` devices by default. Settings include an override; it does not make an incompatible APK work. Some download paths also have vendor-specific checks.
- Root access is not a blanket prerequisite for opening POGO Manager or checking versions. Individual companion workflows may require root or LSPosed; consult their documentation before use.
- The app includes Google Mobile Ads and OneSignal push-notification integration, and uses Google Play services for location. It is not an offline-only or ad-free application.

OEM behavior can differ. In particular, the launcher-icon workaround still needs the device-specific verification recorded in [the task list](../TODO.md); it is not a claim of universal MIUI compatibility.

## Installation

1. Download an APK from [GitHub Releases](https://github.com/lokey0905/POGO_Manager/releases) and read the notes for that release.
2. Open the APK using Android's package installer. If prompted, allow installation from the browser or file manager you used, after verifying the source.
3. Open POGO Manager and grant permissions as needed for the features you use.

The existing [MediaFire backup](https://www.mediafire.com/file/5mdun6u78jr96rw/app-debug.apk/file) is retained as an alternative link; it is not guaranteed to match the latest GitHub release. The APK checked into `app/release/` is an older build, not the recommended latest download.

To build a development APK, see [Building from source](development.en.md#building-from-source). A locally signed build may not install over a published APK if their signing certificates differ; do not assume an in-place replacement will preserve access to existing app data.

## Usage

1. Open a game page and wait for its version checks, or pull down to refresh.
2. On Pokémon GO or Monster Hunter Now, select the target game version before reviewing tool compatibility. Expand the full support lists if needed.
3. Use the app cards to open an installed app or follow its download/update link. POGO Manager does not silently install or update third-party APKs.
4. Use the settings button in the page toolbar to configure download sources and test channels. Home contains device checks; Shortcuts contains utilities and guide links.

Version support, installed version, and latest app version are different pieces of information. A positive support result reflects the retrieved support data, not a successful end-to-end test of that tool on your device.

Detailed instructions are available in the [Traditional Chinese guide index](../docs/README.md).
