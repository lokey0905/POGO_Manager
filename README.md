# POGO Manager

繁體中文 | [English](README.en.md)

POGO Manager 是管理 Pokémon GO、Monster Hunter Now 與 Pikmin Bloom 相關應用程式的 Android 工具，提供版本查詢、支援版本比對、下載連結及裝置檢測。介面與使用文件以繁體中文為主。

## 功能

- 應用程式管理：查詢安裝版本與可取得的最新版本，提供下載、開啟、設定及解除安裝入口。
- 版本相容性：依選定的遊戲版本比對輔助工具支援情況；PGTools 與 MHNTools 可切換正式版、測試版通道。
- 裝置檢測：顯示系統資訊、定位狀態及 root／Magisk 跡象，並提供外部完整性與環境檢測工具入口。
- 實用工具：使用教學、冷卻計算器、桌面捷徑與定位設定小工具。
- 介面：支援深色模式、動態色彩及手機、平板布局。

## 支援遊戲

| 遊戲 | 整合內容 |
| --- | --- |
| Pokémon GO | 遊戲版本選擇、PGTools／Pokemod／PGSharp Root 支援版本比對，以及相關工具的版本查詢與下載 |
| Monster Hunter Now | 遊戲版本選擇、MHNTools 正式版與測試版支援版本比對，以及相關工具下載 |
| Pikmin Bloom | 遊戲、定位與步數工具、Google Fit、Health Connect 及安裝工具清單 |

版本查詢與相容性比對的支援範圍依工具而異。完整清單及限制請參閱[整合與使用指南](docs/app-guide.md)。

## 系統需求

- Android 9（API 28）以上。
- 版本查詢與下載需連線至網際網路。
- 多數遊戲及工具的下載預設限制為 `arm64-v8a` 裝置；略過檢查不會改變 APK 的架構需求。
- 一般版本查詢無須 root。個別第三方工具可能另有 root、LSPosed 或 Android 版本需求。
- 定位檢測需授予位置權限並啟用定位服務。

本應用程式整合 Google Mobile Ads 廣告、OneSignal 推播及 Google Play services 定位服務。

## 下載與安裝

請至 [GitHub Releases](https://github.com/lokey0905/POGO_Manager/releases) 下載 APK，依 Android 系統提示安裝。本文件對應目前原始碼，發布版本的功能與變更以各版 Release 說明為準。

自行建置與發布版的簽章可能不同，安裝前請確認能否覆蓋既有版本。安裝步驟、權限及歷史備份資訊見[安裝指南](docs/app-guide.md#安裝)。

## 畫面截圖

<details>
<summary>舊版畫面</summary>

以下截圖取自歷史版本，與目前介面有所差異。

<img src="docs/images/root/Screenshot_1.jpg" alt="舊版首頁" width="240" />
<img src="docs/images/root/Screenshot_2_1.jpg" alt="舊版寶可夢頁面" width="240" />
<img src="docs/images/root/Screenshot_3_1.jpg" alt="舊版魔物獵人頁面" width="240" />

</details>

## 開發與建置

專案以 Kotlin、Java 開發，介面採用 AndroidX Fragment、XML Layout、View Binding 與 Material Components。網路及非同步處理使用 OkHttp、Coroutines 與 Protocol Buffers；部分裝置檢測透過 C／JNI 及隔離服務實作，使用 CMake 建置。

建置環境需安裝 Git、JDK 21、Android SDK、NDK 與 CMake。工具鏈版本及 SDK 路徑設定請參閱 [Gradle 設定](app/build.gradle)與[開發文件](docs/development.md)。首次建置需下載工具鏈及相依套件。

在專案根目錄執行：

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Linux／macOS：

```sh
chmod +x gradlew
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Debug APK 輸出至 `app/build/outputs/apk/debug/app-debug.apk`。簽章、記憶體設定與測試說明見[建置文件](docs/development.md#從原始碼建置)。

## 維護

維護工作包括 Android 版本適配、上游版本解析與下載來源更新、相依套件升級、崩潰修復及介面調整。上游服務變更可能影響版本查詢或下載功能；已知待辦與測試結果分別記錄於 [TODO.md](TODO.md) 和[驗證紀錄](docs/development.md#維護內容)。

## 貢獻

問題回報與功能建議請提交至 [Issues](https://github.com/lokey0905/POGO_Manager/issues)。提交 pull request 前，請參閱 [CONTRIBUTING.md](CONTRIBUTING.md)，附上修改說明及測試結果。涉及較大範圍的功能或架構變更，請先開啟 issue 討論。

## 文件

- [使用說明](docs/README.md)
- [寶可夢](docs/pokemon_go.md) · [魔物獵人](docs/monster_hunter_now.md) · [皮克敏](docs/pikmin_bloom.md) · [定位工具](docs/location_only.md)
- [整合與相容性限制](docs/app-guide.md)
- [開發與建置](docs/development.md)

## 第三方軟體

第三方程式碼、素材與相依套件的來源及授權聲明記錄於 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。開發過程使用的參考專案及外部工具另見[參考資料](docs/references.md)。

## 免責聲明

本專案為非官方社群專案，與相關遊戲公司及權利人無隸屬或背書關係。第三方商標與圖示歸原權利人所有。裝置檢測與版本資訊僅供參考，不構成裝置安全或帳號使用保證；使用者應遵守適用法律及服務條款。完整聲明見 [DISCLAIMER.md](DISCLAIMER.md)。

## 授權

本專案目前尚未設定專案層級授權。

第三方內容依各自授權條款使用。相關範圍及待確認事項見 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
