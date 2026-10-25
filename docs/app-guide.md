# 詳細使用與相容性說明

繁體中文 | [English](app-guide.en.md) · [返回首頁](../README.md)

本頁保留原 README 的操作說明、工具差異與下載限制；目前原始碼與已發布 APK 可能不同。

## 主要功能

### APP 版本與下載

- 偵測已設定 APP 的安裝版本，並在有遠端來源時比對版本資訊。
- 選擇目標 Pokémon GO 或 Monster Hunter Now 版本，查看輔助工具相容性；資料不足時顯示尚未確認，取得資料時可顯示最高支援版本。完整支援列表可按需展開。
- 同時檢查 PGTools 與 MHNTools 的正式版及測試版資訊，並以開關選擇下載通道。
- 提供下載頁面、開啟 APP、應用程式設定及系統解除安裝入口。部分項目僅提供下載連結，列入清單不代表具備自動版本或相容性偵測。
- 可重新整理版本資訊，並區分「APP 最新版本」與「是否支援目標遊戲版本」。

### 設備檢測與實用工具

- 顯示設備與 Android 資訊，檢查定位／網路定位來源設定、模擬位置標記及 root／Magisk 跡象。
- 經確認對話框後開啟外部完整性與環境檢測工具。結果僅供參考，不代表遊戲相容性或設備安全保證。
- 提供系統設定、遊戲 APP 設定、使用教學、網頁冷卻計算器與 GPX／檔案下載入口。支援的項目可建立桌面捷徑，另有定位精確度設定小工具。
- 採用 Material 風格，支援深色模式、可用裝置上的動態色彩與響應式布局；寬度足夠的平板橫向畫面使用左側導航列。

## 支援的 APP 與遊戲

此處的「支援」是指提供整合功能或清單項目，不代表對第三方 APP 的背書，也不保證適用所有設備或遊戲版本。

| 遊戲／用途 | 整合項目 | 相容性資訊 |
| --- | --- | --- |
| Pokémon GO | 一般版與三星版；PGTools、Pokemod、PGSharp Root、PolygonX、Data Hub、GPS JoyStick、DeFit、PokeList、WeCatch、Galaxy Store、APKMirror Installer | PGTools 正式／測試版、Pokemod、PGSharp Root 可查詢目標遊戲版本相容性。PolygonX 僅檢查自身 APP 版本，不查詢遠端 Pokémon GO 支援列表。 |
| Monster Hunter Now | 遊戲下載、MHNTools、Hylianer、GPS JoyStick、APKMirror Installer | MHNTools 正式／測試版可查詢目標遊戲版本相容性。Hylianer 提供下載／開啟入口，沒有自動最新版檢查。 |
| Pikmin Bloom | Pikmin Bloom、GPS JoyStick、DeFit、Fitness Hooks、Google Fit、Health Connect、APKMirror Installer | 提供 APP 版本資訊與下載清單，沒有皮克敏遊戲相容性對照表。Google Fit 與 Health Connect 使用商店／系統入口，不查詢遠端版本。 |

皮克敏下載會開啟 APKMirror；Google Fit 與 Health Connect 的下載連結會開啟 Google Play。在適用的 Android 版本上，Health Connect 以系統元件方式開啟。GPS JoyStick 依本專案設定的套件名稱辨識，其他套件名稱的安裝版本可能無法被識別。

## 系統需求與相容性限制

- 本 APP 最低需求為 **Android 9／API 28**。各輔助工具另有 Android 版本、處理器架構、root 或框架需求。
- 版本查詢與下載連結需要網路，並依賴上游 API、網站、商店及版本資料。缺少或過期的資料不能作為相容性依據。
- 定位測試需要位置權限及已啟用的定位服務；套件可見性用於偵測安裝狀態與版本。是否能釘選捷徑或小工具取決於桌面程式的支援。
- 多數遊戲／工具下載預設僅允許 `arm64-v8a` 裝置。設定中可略過此限制，但不會因此讓不相容的 APK 正常執行；部分下載另有廠商條件檢查。
- 開啟 POGO Manager 或查詢版本並不一律需要 root；個別輔助工具可能需要 root 或 LSPosed，使用前請閱讀其文件。
- APP 整合 Google Mobile Ads 廣告與 OneSignal 推播，定位功能使用 Google Play services；並非純離線或無廣告的應用程式。

不同廠商的系統行為可能不同。桌面捷徑圖示修正仍有[待辦清單](../TODO.md)記錄的特定裝置驗收項目，不代表已確認所有 MIUI 設備皆正常。

## 安裝

1. 從 [GitHub Releases](https://github.com/lokey0905/POGO_Manager/releases) 下載 APK，並閱讀對應版本說明。
2. 使用 Android 套件安裝程式開啟 APK。若系統要求，請確認來源後，允許所用瀏覽器或檔案管理器安裝應用程式。
3. 開啟 POGO Manager，依使用的功能授予所需權限。

保留既有 [MediaFire 備份下載](https://www.mediafire.com/file/5mdun6u78jr96rw/app-debug.apk/file)作為替代入口，但不保證與最新 GitHub Release 一致。儲存庫中 `app/release/` 的 APK 是舊版建置，不建議將它當成最新版下載。

自行建置請參閱[從原始碼建置](development.md#從原始碼建置)。若本地建置與已發布 APK 的簽章不同，可能無法覆蓋安裝；不能假設可直接替換並沿用既有 APP 資料。

## 基本使用

1. 開啟遊戲頁面，等待版本檢查，或下拉重新整理。
2. 在寶可夢或魔物獵人頁面，先選擇目標遊戲版本，再查看工具相容性；需要時可展開完整支援列表。
3. 使用 APP 卡片開啟已安裝應用程式，或前往下載／更新連結。POGO Manager 不會靜默安裝或更新第三方 APK。
4. 各頁工具列的設定按鈕可調整下載來源與測試版通道。首頁提供設備檢測，捷徑頁提供工具與教學入口。

「支援版本」、「已安裝版本」與「APP 最新版本」是不同資訊。顯示支援代表符合目前取得的支援資料，不代表已在你的裝置上完成該工具的實際運作驗證。

詳細步驟請參閱[使用說明總入口](../docs/README.md)。
