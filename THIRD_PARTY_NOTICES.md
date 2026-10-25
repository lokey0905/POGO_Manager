# Third-party notices

本文件記錄已確認包含、修改或作為實際依賴使用的第三方內容。外部 APP、下載連結與開發參考另見 [references](docs/references.md)，不列為 bundled code。

**Status:** No project-level license has been adopted. Attribution work is in progress; this is not a complete binary SBOM or confirmation that all APK distribution requirements have been met. See the [unresolved issues below](#unresolved-included-material-and-distribution-limits). Nothing here grants rights beyond the original licenses or relicenses third-party assets.

## DetectMagiskHide

- Project: [DetectMagiskHide](https://github.com/darvincisec/DetectMagiskHide)
- Compared revision: [dfecbb5ef6c92916bd5e96f22251e07362d47be9](https://github.com/darvincisec/DetectMagiskHide/tree/dfecbb5ef6c92916bd5e96f22251e07362d47be9). The exact original import revision is unknown.
- Original license: **Apache License 2.0** — [upstream LICENSE](https://github.com/darvincisec/DetectMagiskHide/blob/dfecbb5ef6c92916bd5e96f22251e07362d47be9/LICENSE), [local full text](LICENSES/Apache-2.0.txt).
- Original copyright, reproduced exactly from upstream: **Copyright [2020] [Darvin]**.
- No separate upstream NOTICE was found in the inspected repository/history.

Files containing derived portions:

| POGO Manager path | Scope and modifications |
| --- | --- |
| [app/src/main/c/native-lib.c](app/src/main/c/native-lib.c) | Native detection; JNI package/declaration and integration comments adapted. |
| [app/src/main/java/app/lokey0905/location/IsolatedService.java](app/src/main/java/app/lokey0905/location/IsolatedService.java) | Isolated-service detection, Binder and mount checks; package/constants adapted. |
| [app/src/main/java/app/lokey0905/location/Native.java](app/src/main/java/app/lokey0905/location/Native.java) | Native bridge; package changed. |
| [app/src/main/aidl/app/lokey0905/location/IIsolatedService.aidl](app/src/main/aidl/app/lokey0905/location/IIsolatedService.aidl) | Binder interface; package changed. |
| [app/src/main/java/app/lokey0905/location/AppZygotePreload.java](app/src/main/java/app/lokey0905/location/AppZygotePreload.java) | Preload callback source; package changed. Inclusion does not imply the callback is registered in the current manifest. |
| [app/src/main/java/app/lokey0905/location/MainActivity.kt](app/src/main/java/app/lokey0905/location/MainActivity.kt) | **Only isolated-service binding and Magisk-check portions**, ported from upstream DetectMagisk.java to Kotlin and integrated into local presentation/lifecycle code. Not the entire Activity. |

Modified for integration into POGO Manager. Original portions retain Apache-2.0 attribution. Attribution is limited to the six entries above; provenance of the CMake template and Manifest service configuration remains uncertain.

## AndroidX preference layout and Stack Overflow adaptation

Included in [app/src/main/res/layout/preference_switch.xml](app/src/main/res/layout/preference_switch.xml):

1. AndroidX [preference_widget_switch_compat.xml](https://github.com/androidx/androidx/blob/005e9694795cee9a42375d80b0d813af9e700ac1/preference/preference/res/layout/preference_widget_switch_compat.xml), revision `005e9694795cee9a42375d80b0d813af9e700ac1`.
   - **Copyright (C) 2015 The Android Open Source Project**
   - [Apache-2.0](LICENSES/Apache-2.0.txt).
2. Ebrahim Byagowi's answer to [“New Android 12+ MaterialSwitch and androidx.preference”](https://stackoverflow.com/a/73782598), published 2022-09-20; inspected revision last edited 2023-05-19.
   - **CC BY-SA 4.0**, as identified by the Stack Exchange API.
   - [License description](https://creativecommons.org/licenses/by-sa/4.0/) and [full legal text](LICENSES/CC-BY-SA-4.0.txt).

Modified for MaterialSwitch integration and XML formatting. The original two source comments remain. AndroidX-derived content and the Stack Overflow contribution retain their respective notices; the entire contribution is not represented as Apache-only. No endorsement by the author is implied.

## Google Material icons and symbols

- Project: [Google Material design icons](https://github.com/google/material-design-icons)
- Compared revision: [10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7](https://github.com/google/material-design-icons/tree/10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7)
- License: **Apache-2.0** — [upstream LICENSE](https://github.com/google/material-design-icons/blob/10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7/LICENSE), [local full text](LICENSES/Apache-2.0.txt).

The Google vector artwork is retained. POGO Manager uses app-specific tint, fill-color/theme and display settings; the launcher foreground additionally scales and centers the catching_pokemon artwork. Some path strings have equivalent coordinate/serialization forms; this is not evidence that the artwork was redesigned. No unsupported copyright year is assigned.

Most baseline icons correspond to the upstream `android/<category>/<icon>/materialicons/black/res/drawable/` assets. `outline_switch_account_24.xml` corresponds to `symbols/android/switch_account/materialsymbolsoutlined/switch_account_24px.xml`; `ic_youtube_activity.xml` corresponds to `symbols/android/youtube_activity/materialsymbolsoutlined/youtube_activity_48px.xml`. The launcher foreground uses `catching_pokemon`.

The following 43 files in [app/src/main/res/drawable/](app/src/main/res/drawable/) are covered by this attribution:

```text
baseline_app_blocking_24.xml
baseline_app_shortcut_24.xml
baseline_apps_24.xml
baseline_calculate_24.xml
baseline_call_split_24.xml
baseline_developer_mode_24.xml
baseline_edit_location_24.xml
baseline_expand_less_24.xml
baseline_expand_more_24.xml
baseline_games_24.xml
baseline_insert_drive_file_24.xml
baseline_key_24.xml
baseline_local_florist_24.xml
baseline_map_24.xml
baseline_menu_book_24.xml
baseline_mode_standby_24.xml
baseline_mosque_24.xml
baseline_not_listed_location_24.xml
baseline_notifications_off_24.xml
baseline_open_in_browser_24.xml
baseline_pets_24.xml
baseline_radar_24.xml
baseline_settings_24.xml
baseline_share_24.xml
baseline_shield_24.xml
baseline_splitscreen_24.xml
ic_baseline_android_24.xml
ic_baseline_browser_updated_24.xml
ic_baseline_catching_pokemon_24.xml
ic_baseline_code_24.xml
ic_baseline_download_24.xml
ic_baseline_email_24.xml
ic_baseline_house_24.xml
ic_baseline_info_24.xml
ic_baseline_mark_unread_chat_alt_24.xml
ic_baseline_more_vert_24.xml
ic_baseline_settings_24.xml
ic_baseline_share_location_24.xml
ic_baseline_shopping_bag_24.xml
ic_baseline_sync_disabled_24.xml
ic_launcher_foreground.xml
ic_youtube_activity.xml
outline_switch_account_24.xml
```

Material icon attribution is consolidated in this section, with the full license in [LICENSES/Apache-2.0.txt](LICENSES/Apache-2.0.txt). This entry does **not** license Google Fit/Health Connect store images, game logos, other raster artwork, or trademarks; those are separate [unresolved asset questions](#unresolved-included-material-and-distribution-limits).

## Gradle wrapper

Included tooling, not an Android runtime library:

- [gradlew](gradlew)
- [gradlew.bat](gradlew.bat)
- [gradle/wrapper/gradle-wrapper.jar](gradle/wrapper/gradle-wrapper.jar)

Project: [Gradle](https://github.com/gradle/gradle). License: [Apache-2.0](LICENSES/Apache-2.0.txt).

The scripts retain **Copyright 2015 the original author or authors** and their original license notices. The wrapper JAR contains Gradle wrapper/CLI code. Its SHA-256 is `e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637`; the initial distribution configuration was 7.3.3. Original 7.3.3 wrapper/CLI source headers include Copyright 2007 / 2010 the original author or authors. The current distribution URL is not the version identity of the checked-in JAR.

This entry does not claim that every component of a full Gradle distribution is vendored in this wrapper JAR.

## Runtime dependencies with additional license texts

These SDKs/libraries are actual Gradle dependencies, not third-party apps that a user installs separately. Versions below are the audited configuration/resolution snapshot; [app/build.gradle](app/build.gradle) remains the configuration source of truth.

### OneSignal 5.7.7

- Project: [OneSignal Android SDK](https://github.com/OneSignal/OneSignal-Android-SDK/tree/5.7.7)
- **Copyright 2016 OneSignal**
- License: **Modified MIT**, not standard MIT.
- [Complete upstream text](https://github.com/OneSignal/OneSignal-Android-SDK/blob/5.7.7/LICENSE) is retained in [LICENSES/OneSignal-5.7.7.txt](LICENSES/OneSignal-5.7.7.txt), including its Google GcmClient, RootTools and ShortcutBadger notices.

The license restricts use of substantial portions to connection with OneSignal services. This document does not waive that condition or change the SDK's license terms.

### Protocol Buffers Java lite 4.34.1

- Project: [Protocol Buffers](https://github.com/protocolbuffers/protobuf)
- **Copyright 2008 Google Inc.  All rights reserved.**
- License: **BSD-3-Clause**, verified against the Maven parent and upstream tag v34.1.
- [Full notice and license](LICENSES/Protobuf-4.34.1.txt).

This describes the runtime library, not the provenance of POGO Manager's own protocol schema files.

### kotlinx.coroutines 1.9.0

- Project: [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines/tree/1.9.0)
- License: [Apache-2.0](LICENSES/Apache-2.0.txt).
- Original [license/NOTICE.txt](https://github.com/Kotlin/kotlinx.coroutines/blob/1.9.0/license/NOTICE.txt) is reproduced below:

```text
=========================================================================
==  NOTICE file corresponding to the section 4 d of                    ==
==  the Apache License, Version 2.0,                                   ==
==  in this case for the kotlinx.coroutines library.                   ==
=========================================================================

kotlinx.coroutines library.
Copyright 2016-2024 JetBrains s.r.o and contributors
```

### Google Mobile Ads and Play services Location

- Dependencies: `com.google.android.gms:play-services-ads:25.2.0` and `com.google.android.gms:play-services-location:21.3.0`.
- The official [Ads POM](https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-ads/25.2.0/play-services-ads-25.2.0.pom) and [Location POM](https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-location/21.3.0/play-services-location-21.3.0.pom) reference the [Android SDK License Agreement](https://developer.android.com/studio/terms).
- See also [Mobile Ads SDK terms](https://developers.google.com/admob/android/terms).

These SDKs are **not described as Apache-2.0 libraries**. Their included open-source components have separate notices, available in the AAR third_party_licenses resources. These resources are not automatically included by adding this Markdown file. Compliance with the applicable SDK distribution terms and notice delivery requirements has not been fully verified; see the [distribution limits below](#unresolved-included-material-and-distribution-limits).

## Other confirmed runtime dependency families

| Dependency | Verified main license / attribution source |
| --- | --- |
| AndroidX libraries | Apache-2.0; original notices vary by source file. Their use does not mean this app's UI is Compose. |
| Material Components Android 1.13.0 | [Apache-2.0](https://github.com/material-components/material-components-android/blob/1.13.0/LICENSE). For example, MaterialButton retains Copyright (C) 2017 The Android Open Source Project; this is not a single copyright year for the entire library. |
| OkHttp 5.3.2 | [Apache-2.0](https://github.com/square/okhttp/blob/parent-5.3.2/LICENSE.txt); upstream README: Copyright 2019 Square, Inc.; OkHttpClient source: Copyright (C) 2012 Square, Inc. |
| collapsingtoolbarlayout-subtitle 1.5.0 | [Apache-2.0](https://github.com/hendraanggrian/collapsingtoolbarlayout-subtitle/blob/1.5.0/LICENSE), project by Hendra Anggrian. No copyright year is invented from the template placeholder. |
| Kotlin stdlib 2.2.21 | Main license Apache-2.0; [upstream copyright](https://github.com/JetBrains/kotlin/blob/v2.2.21/license/COPYRIGHT.txt): Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors. Some actual source headers use 2010-2025 and must not be normalized away. |

A copy of the Apache license is provided [here](LICENSES/Apache-2.0.txt). This table is **not** the complete transitive/nested notice inventory. In particular, Kotlin's [license index](https://github.com/JetBrains/kotlin/blob/v2.2.21/license/README.md) identifies GWT/Guava, ThreeTen-derived BSD code, and Boost-licensed MathJVM portions. The Kotlin Compiler distribution NOTICE must not be mislabeled as a stdlib-only NOTICE.

## Unresolved included material and distribution limits

Fitness Hooks artwork and other store/game images are present, but a license grant covering their redistribution has not been established. Protocol-schema provenance and the legacy APK also need clarification. The licenses listed in this document do **not** establish permission to redistribute these unresolved materials.

Before publishing a license-cleared APK, verify the applicable SDK distribution terms, identify the actual bundled transitive/nested components after shrinking, and deliver their required notices with the distribution. Root documentation alone does not complete that work.
