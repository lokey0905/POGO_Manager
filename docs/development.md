# 開發與維護說明

繁體中文 | [English](development.en.md) · [返回首頁](../README.md)

## 開發與技術架構

專案包含一個 Android 應用程式模組 `:app`，Application ID 為 `app.lokey0905.location`。

- Kotlin、Java、AndroidX Fragment、XML Layout、View Binding 與 Material Components；目前畫面並非以 Jetpack Compose 實作。
- 使用 Kotlin 協程、OkHttp、HTTP／JSON 整合與 Protocol Buffers 進行上游服務通訊。
- 部分設備檢測使用 C／JNI 與隔離服務，原生程式碼透過 CMake 建置。
- 使用 JUnit 4 測試版本比較、相容性、上游資料解析、導航狀態、清單操作及布局／圖示規則。

主要目錄：

| 路徑 | 內容 |
| --- | --- |
| [app/src/main/java/app/lokey0905/location/](../app/src/main/java/app/lokey0905/location/) | Activity、Fragment、API 整合、APP 清單、版本邏輯與工具 |
| [app/src/main/res/](../app/src/main/res/) | XML 布局、字串、主題、圖示與端點資源 |
| [app/src/main/c/](../app/src/main/c/) | 原生設備檢測程式碼 |
| [app/src/test/](../app/src/test/) | JVM 單元測試 |
| [docs/](../docs/README.md) | 使用教學、圖片與各項驗證紀錄 |

## 從原始碼建置

### 準備環境

- 安裝 Git 與 JDK 21。命令列建置請設定 `JAVA_HOME`，或在 Android Studio 設定 Gradle JDK。已納入版本控制的 [daemon JVM 條件](../gradle/gradle-daemon-jvm.properties)指定 JetBrains JDK 21，Gradle 可能另行下載此工具鏈。
- 安裝 Android SDK Platform **37**、SDK Build Tools、Android NDK 與 CMake **3.18.1**。可使用 Android Studio 的 SDK Manager 或 Android 命令列工具。專案未固定 NDK 版本，由 AGP 選擇預設版本。
- 使用 Android Studio 設定 SDK 路徑，或在不納入版本控制的 `local.properties` 中設定 `sdk.dir`。請勿提交本機路徑或簽章金鑰。
- 首次建置需連線下載 Gradle、工具鏈與相依套件。

目前設定使用 Gradle **9.3.1**、Android Gradle Plugin **9.1.1** 與 Kotlin Android plugin **2.2.10**；Java／Kotlin 位元碼目標為 17。APP 模組的 compile SDK 與 target SDK 均為 37，請以 [app/build.gradle](../app/build.gradle) 為準。

### 建置與測試

複製儲存庫並進入目錄：

```sh
git clone https://github.com/lokey0905/POGO_Manager.git
cd POGO_Manager
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Linux 或 macOS（使用 `sh` 可避免 wrapper 缺少執行權限的問題）：

```sh
sh ./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Debug APK 位於 `app/build/outputs/apk/debug/app-debug.apk`；單元測試報告位於 `app/build/reports/tests/testDebugUnitTest/index.html`。

[gradle.properties](../gradle.properties) 目前允許 Gradle 使用最大 12 GiB 堆積記憶體。記憶體較少的開發環境可在建置命令後加上：

```text
--max-workers=2 "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8"
```

Release 建置已啟用程式碼縮減，但儲存庫未提供 Release 簽章設定。發布時需另外設定簽章，建置成功不等於已產生可發布的簽章 APK。JVM 單元測試亦不能取代裝置測試。

### CI 現況

既有 [Android CI workflow](../.github/workflows/android.yml) 會在 `master` 的 push／pull request 涉及 `docs/**` 變更時執行 `./gradlew build`，未涵蓋所有程式碼變更。它設定 JDK 11，但 wrapper／client JVM 與實際執行建置的 daemon JVM 可以不同：Gradle 9.3.1 的 client 支援 Java 8 以上，而本專案的 [daemon JVM 條件](../gradle/gradle-daemon-jvm.properties)指定 JetBrains JDK 21。因此不能只由 workflow 的 JDK 11 設定判定建置必然失敗；仍需確認 CI 能否找到或下載指定的 daemon JDK，以及完整建置結果。詳見 Gradle 的 [Java 相容性](https://docs.gradle.org/9.3.1/userguide/compatibility.html#java_runtime)與 [daemon JVM 條件](https://docs.gradle.org/9.3.1/userguide/gradle_daemon.html#sec:daemon_jvm_criteria)。本次未執行或核實 GitHub Actions run，準備修改時仍應執行本地建置與測試。

## 維護內容

Git history 與 [Release 說明](https://github.com/lokey0905/POGO_Manager/releases)記錄了 Android SDK／相依套件更新、上游版本解析與下載來源修正、崩潰修復及 UI 調整。這些整合需要隨 Android 與上游 APP 的變化持續維護；即使 POGO Manager 本身沒有更新，上游變更仍可能使功能失效。

儲存庫另有[布局與捷徑驗證紀錄](../docs/ui-cd-validation.md)、[皮克敏／導航驗證](../docs/pikmin-navigation-validation.md)及[側邊導航間距驗證](../docs/navigation-rail-spacing-validation.md)。這些文件記錄特定測試與限制，不代表對所有裝置的保證。[TODO.md](../TODO.md) 追蹤待辦事項，未完成項目不應視為現有功能。

## CI 後續建議（本次未修改 workflow）

- 明確管理 workflow 的 client JDK 與 daemon JDK，核對 JetBrains JDK 21 的取得與快取；可將 client JDK 也更新為 21 以減少環境差異，但不能把目前 client 使用 JDK 11 直接視為建置失敗證據。
- 重新設計 `paths` 篩選，涵蓋 APP 原始碼、測試、Gradle 設定與 workflow 本身，而非只檢查 `docs/**`。
- 明確執行 debug 建置與 JVM 單元測試，保存測試報告；lint 應另列步驟並處理既有問題，不把排除部分來源的 lint 視為完整通過。
- 核對 SDK／NDK／CMake 安裝與快取，更新 Actions 相依版本並考慮固定 commit SHA。不得將 release 金鑰放入 repository。

以上是後續工作建議，不代表本次已修復或執行 GitHub Actions。此輪僅檢查本地設定。
