# Development and maintenance

[繁體中文](development.md) | English · [Project overview](../README.en.md)

## Development and tech stack

The project has one Android application module, `:app`, with application ID `app.lokey0905.location`.

- Kotlin and Java, AndroidX Fragments, XML layouts, View Binding, and Material Components. The current screens are not a Jetpack Compose UI.
- Kotlin coroutines, OkHttp, HTTP/JSON integrations, and Protocol Buffers for upstream communication.
- C/JNI and an isolated service for parts of the device checks, built with CMake.
- JUnit 4 tests for version comparison, compatibility, upstream parsing, navigation state, catalog actions, and layout/icon policies.

Key locations:

| Path | Contents |
| --- | --- |
| [`app/src/main/java/app/lokey0905/location/`](../app/src/main/java/app/lokey0905/location/) | Activity, fragments, API integrations, catalog, version logic, and utilities |
| [`app/src/main/res/`](../app/src/main/res/) | XML layouts, strings, themes, icons, and endpoint resources |
| [`app/src/main/c/`](../app/src/main/c/) | Native device-check code |
| [`app/src/test/`](../app/src/test/) | JVM unit tests |
| [`docs/`](../docs/README.md) | User guides, images, and scoped validation reports |

## Building from source

### Prerequisites

- Git and a JDK 21 installation. Set `JAVA_HOME` for command-line builds, or configure Android Studio's Gradle JDK. The checked-in [daemon criteria](../gradle/gradle-daemon-jvm.properties) request JetBrains JDK 21, which Gradle may provision separately.
- Android SDK Platform **37**, SDK Build Tools, the Android NDK, and CMake **3.18.1**. Install them through Android Studio's SDK Manager or the Android command-line tools. The project does not pin an NDK version; AGP selects its default.
- Configure the SDK location through Android Studio or an untracked `local.properties` containing `sdk.dir`. Do not commit machine-specific paths or signing keys.
- Network access for the initial Gradle distribution, toolchain, and dependency downloads.

The current configuration uses Gradle **9.3.1**, Android Gradle Plugin **9.1.1**, and Kotlin Android plugin **2.2.10**. Java/Kotlin bytecode targets 17. The app module's compile and target SDK are both 37; use [`app/build.gradle`](../app/build.gradle) as the source of truth.

### Build and test

Clone and enter the repository:

```sh
git clone https://github.com/lokey0905/POGO_Manager.git
cd POGO_Manager
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

On Linux or macOS (using `sh` also works when the wrapper lacks its executable bit):

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`; the unit-test report is at `app/build/reports/tests/testDebugUnitTest/index.html`.

[`gradle.properties`](../gradle.properties) currently allows a 12 GiB Gradle heap. On a smaller development machine, append these options to the build command:

```text
--max-workers=2 "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8"
```

Release builds have minification enabled but no release signing configuration in this repository. Publishing requires a separate signing setup; a successful build alone is not a signed release. Device testing is separate from the JVM tests.

### CI status

The existing [Android CI workflow](../.github/workflows/android.yml) runs `./gradlew build` for `docs/**` changes on `master` pushes and pull requests, so it does not cover all source changes. It configures JDK 11, but the wrapper/client JVM and the daemon JVM that executes the build can differ: Gradle 9.3.1's client supports Java 8 and later, while this project's [daemon criteria](../gradle/gradle-daemon-jvm.properties) request JetBrains JDK 21. The workflow's JDK 11 setting alone therefore does not establish a build failure; CI must still be checked for access to the requested daemon JDK and a successful full build. See Gradle's [Java compatibility](https://docs.gradle.org/9.3.1/userguide/compatibility.html#java_runtime) and [daemon JVM criteria](https://docs.gradle.org/9.3.1/userguide/gradle_daemon.html#sec:daemon_jvm_criteria). No GitHub Actions run was executed or verified in this audit. Run the local build and tests when preparing a change.

## Maintenance

The Git history and [release notes](https://github.com/lokey0905/POGO_Manager/releases) record Android SDK/dependency updates, upstream version-parser and download-source fixes, crash fixes, and UI changes. Ongoing maintenance includes keeping those integrations working as Android and upstream apps change; an integration can stop working without a POGO Manager release.

The repository also contains [layout and shortcut validation notes](../docs/ui-cd-validation.md), [Pikmin/navigation validation](../docs/pikmin-navigation-validation.md), and [navigation-rail validation](../docs/navigation-rail-spacing-validation.md). These record specific checks and remaining limitations, not a guarantee for all devices. [TODO.md](../TODO.md) tracks pending work and should not be read as a list of available features.

## Suggested CI follow-up (workflow unchanged)

- Manage the client and daemon JDKs explicitly and check JetBrains JDK 21 provisioning and caching. Updating the client JDK to 21 could reduce environment differences, but the current JDK 11 client setting is not itself evidence of a failed build.
- Revisit path filters to cover app sources, tests, Gradle configuration, and the workflow itself rather than only `docs/**`.
- Run explicit debug build and JVM test tasks and retain reports. Treat lint separately and resolve existing issues; lint with excluded sources is not a complete pass.
- Check SDK/NDK/CMake provisioning and caching, update Actions dependencies, and consider pinning commit SHAs. Keep release keys out of the repository.

These are follow-up recommendations, not changes or GitHub Actions runs performed by this audit. Only the local configuration was inspected.
