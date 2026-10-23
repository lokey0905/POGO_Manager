# 皮克敏頁面與設定入口驗證

日期：2026-10-23；分類與圖示調整更新於 2026-10-24。沿用 XML、Fragment、Material 與現有五格導覽，不新增套件。

## Git 範圍

- 先完成本地提交 `6fb0998`：UI 尺寸還原七檔與 `version_labels.xml`，共八檔。
- 本頁所述皮克敏／設定／MHN Installer 新功能尚未提交，沒有推送遠端。
- 保留既有 `app/build.gradle` 2.29 版本變更、未追蹤 Pokemod 測試及本機工具資料夾。

## 行為與檔案

- `MainActivity.kt`、`navigation/TabNavigationState.kt`：設定改為輔助目的地，返回來源頁；設定中重選來源導航項目也可返回。舊版只保存 Settings 的狀態補上 Home 根。
- `menu/nav.xml`：寶可夢、皮克敏、首頁、魔物獵人、捷徑。底部列與平板左側列共用此順序。
- 五個主要 Fragment 的 toolbar 加設定；Home 原本下載、分享、關於等選單保留。
- `fragment_preferences.xml`：設定工具列與 Up 返回；移除 `xml/preferences.xml` 原本空白的 `PreferenceCategory`，保留通知欄 inset。
- `AppsPikmin.kt`、`fragment_apps_pikmin.xml`、`item_catalog_app.xml`：七項可捲動平面 APP 列表，圖示、簡短描述／名稱、版本與按鈕排列比照 Pokémon 現有風格。依 2026-10-24 要求移除先前搬到副標題的補充說明，文字可換行，無固定卡片高度。
- `CatalogApp.kt`、`item_catalog_section.xml`：皮克敏保留三個獨立分類標題：步數增加工具、健康資料、安裝工具；DeFit／Fitness Hooks 同組，Google Fit／Health Connect 同組。依後續要求移除「遊戲／定位工具」兩個標題及其間距，Pikmin／搖桿 APP 本身不變。沿用 Pokémon 分區標題的 16sp／Material 主色，並標記為無障礙標題。MHN 單獨的 Installer 不額外插入分類。
- `CatalogAppIcon.kt`、兩個遊戲頁 XML 的 `gps_icon`：統一使用隨 APP 打包的圖示，不隨安裝狀態切換。已撤除先前新增的 `InstalledAppIcon.kt` 與 onResume 動態讀取，避免未安裝時回退舊圖。沒有更換套件名稱或加入執行期圖示網路請求。
- `AppsMHN.kt`／layout：僅補上共用 Installer 區與設定入口，不改既有版本選擇、相容性或下載邏輯。
- `catalog/CatalogCardsController.kt`：各遠端查詢並行、逐筆呈現；五分鐘快取；下拉強制刷新；view lifecycle 取消與 generation 防止舊結果覆寫。動態純呈現卡不保存重複 child ID 的 View state，外層捲動容器仍自行保存狀態。
- `CatalogActionPolicy.kt`：沿用既有數字版本比較器。未安裝為下載、較舊為更新，其餘為開啟；更多選單也保留開啟 APP。未安裝隱藏移除／更多。
- 新文字集中於 `catalog_strings.xml`、`navigation.xml`；套件／來源集中於 `catalog_sources.xml`。不硬編碼最新版本。

## 來源與分類

| 項目 | 分類 | 版本／下載來源 |
| --- | --- | --- |
| Pikmin Bloom | 原版皮克敏客戶端 | 既有 APKPure API 查最新版本；下載開啟使用者指定的 APKMirror listing |
| GPS JoyStick | 可修改手機定位 | 既有 appInfo `pogo.joystick`，保留原專案改名套件與 ARM64 下載限制 |
| DeFit | 步數增加工具 | 既有 API 與 APKMirror URL 格式 |
| Fitness Hooks | 步數增加工具 | 官方 GitHub 最新正式 release；驗證版本及該 repo/tag 的 APK asset |
| Google Fit | 健身資料記錄與同步 | 不猜測最新版本；下載頁連 Google Play |
| Health Connect | 健康資料整合 | Android 13 以下連 Google Play；14 以上為系統元件，僅開啟系統設定，不提供移除或 APK 更新 |
| APKMirror Installer | 安裝工具 | 既有 API 與 APKMirror URL 格式；同時出現在皮克敏及 MHN |

Fitness Hooks 套件名稱保留大小寫 `com.lokey0905.FitnessHooks`。APP 最新版本不代表遊戲支援版本，沒有加入虛構的相容性摘要。

下載／更新只開來源頁或官方 release APK，保留確認對話框；不在背景安裝。Google Play 使用專用提示文字，不顯示 APK 網頁下載教學。

## 官方圖示

所有目錄圖示固定使用打包資產，不被本機 APP 或系統圖示取代；只在建立 APP 項目時設定，不在版本 render／安裝狀態更新時切換。

- [Google Fit](https://play.google.com/store/apps/details?id=com.google.android.apps.fitness)
- [Health Connect](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata)
- 原圖保存在 `drawable-nodpi/catalog_google_fit.png` 與 `catalog_health_connect.png`，未重繪。
- 來源同時核對 `og:image`、主標頭 `img[alt="Icon image"]` 及 `SoftwareApplication.image`，三者為相同資產。
- Pikmin Bloom 新增 `drawable-nodpi/catalog_pikmin.png`：[官方 Play 頁](https://play.google.com/store/apps/details?id=com.nianticlabs.pikmin)，SHA-256 `4EBC4C1F37DCBF679FC499DA94C7720F5186D7544DAE30669693E76DD61479F2`。
- GPS JoyStick 更新共用 `gps_icon.png`：[官方 Play 頁](https://play.google.com/store/apps/details?id=com.theappninjas.fakegpsjoystick)，SHA-256 `249F1A4DF50490AA6F6BBF397CA58171E4CE0820F66AC7B065BF5B7DFF3C15D5`。這兩張皆原樣下載 512×512 PNG，不依安裝情況切換；三個遊戲頁使用同一搖桿資源。保留原專案重命名套件偵測，不改為 Play package。
- Fitness Hooks 複用使用者提供的 `pikmin-hc-a13-lsposed/app/src/main/res/` 原始 launcher foreground 與背景色，前景 XML 內容核對一致；以相同 adaptive-icon 結構打包成 `catalog_fitness`（本專案 minSdk 28），取代通用健身符號。
- DeFit／Installer 現有圖案已對照官方 Play 頁，保持原資源；移除未使用的 `catalog_fitness_24.xml`。導航用花朵符號仍保留，但不再用作 Pikmin APP 圖示。

其他來源：[Pikmin APKMirror](https://www.apkmirror.com/apk/scopely-explore-inc/pikmin-bloom/)、[Fitness Hooks releases](https://github.com/lokey0905/Fitness-Hooks/releases)、[Health Connect 官方可用性說明](https://developer.android.com/health-and-fitness/health-connect/availability)。

## 本機驗證

使用 JDK 21、現有 Gradle wrapper、離線依賴執行：

```text
:app:testDebugUnitTest :app:assembleDebug :app:lintReportDebug
-x :app:lintAnalyzeDebugUnitTest -x :app:lintAnalyzeDebugAndroidTest
--offline --console=plain --max-workers=2
```

- 201 項單元測試，0 failure、0 error、0 skipped。
- 此功能累計新增 14 項來源解析、9 項按鈕／快取策略、6 項導航、5 項分類及 4 項固定圖示映射測試，共 38 項。
- 主要來源 lint 無 error，127 warnings；並非全 test source lint，排除項目如上述指令。
- `assembleDebug` 成功；既有 AGP deprecation 與 debug minify 警告未在本次變更。
- 2026-10-24 最終 debug APK SHA-256：`2CD750742E71CD4EB80EF221E5155A80EE23A8567D0CB5A0AA28AA4B6331FD7D`（固定圖示修正版，尚未覆蓋安裝）。

## Nothing A142 實機紀錄

證據位於忽略的 `app/build/pikmin-20261023/`，每次操作前確認序號與 boot ID。覆蓋安裝保留資料，沒有重啟、清資料、變更遊戲設定，沒有安裝／移除清單中的第三方 APP。

- `02`–`09`：首頁／皮克敏的設定 Up、系統返回與重選來源導航項目通過。
- `20`–`28`：MHN、Pokémon、捷徑的設定返回通過；MHN 原選擇仍為同一版本。
- `05`／`10`／`16`：七個項目各自顯示版本；較舊 Pikmin／Fitness 顯示更新；Installer 未安裝沒有更多／移除；Health Connect 顯示系統元件。
- `11`–`14`：Google Fit 下載頁實際進入 `com.android.vending` 對應詳情頁，只查看後返回，沒有更新或解除安裝。
- `17`：Health Connect 開啟 `com.google.android.healthconnect.controller`，沒有讀取健康資料或變更權限。
- `32`／`33`：改為平面列表與指定導航順序；設定空分類移除後仍保留通知欄安全區。
- `34`／`35`：設定頁旋轉後可返回皮克敏。曾觀察到捲動位置移動；無直接焦點證據，沒有將其誤判為確定根因。後續加入純呈現卡狀態隔離，仍由 NestedScrollView 自行恢復／按新視窗大小限制位置，沒有再疊加手動 scrollY 還原。
- `41`：MHN 最下方 Installer 可見並成功取得版本。
- `52`：官方商店圖示與兩個「步數增加工具」APP 描述呈現正常；當時尚無獨立分區標題，已於 2026-10-24 補上。
- `62`：當時補充說明在副標題位置，版本區只留版本內容；依 2026-10-24 要求，補充說明已移除。
- `63`–`66`：暫時模擬 1280×800dp 平板橫向，五格左側導航順序正確；設定開啟與返回通過，列表置中限寬、可捲動。
- `67`／`68`：手機 200% 字體下標題、按鈕與副標題可辨識、正常換行，沒有水平溢位。
- `69`：尺寸、密度、字體、旋轉全部還原為測試前值：1084×2412、420dpi、1.0、自由旋轉（user_rotation 1）；boot ID 未變。
- `74`–`76`：MHN Installer 下載確認可開啟並取消，返回皮克敏；沒有下載／安裝 APK。早先 `42` 的點擊因頁面與舊座標不一致未算通過，此處重新讀取當前頁後重測。
- 23:20 起的 crash buffer 沒有本 APP FATAL EXCEPTION，最終 APP 程序仍在執行。

本機 ADB 服務曾重新啟動，已通知使用者；裝置重新連上且 boot ID 一致，沒有觀察到手機重啟。CLI 截圖與 UI dump 工具僅用於本次測試。

## 2026-10-24：分類、說明與搖桿圖示實測

仍使用同一隻 Nothing A142，每次操作前檢查序號與原 boot ID（識別值只保留在本機忽略的測試證據）。`80` 階層擷取失敗且截圖全黑，後確認手機為 Dozing；喚醒後 `81` 顯示鎖定，停止 UI 操作並由使用者解鎖，沒有變更鎖定設定。`82` 覆蓋安裝成功，保留資料，冷啟動正常。

### Action: 開啟皮克敏並確認分類標題 ✅

- 根據 `82` 的導航元素中心執行 `adb -s <device-serial> shell input tap 338 2230`。
- `83` 階層與截圖確認「遊戲／定位工具／步數增加工具」為獨立標題。
- 依當前清單範圍執行慢速捲動 `shell input swipe 540 1840 540 760 550`，`84` 確認 DeFit／Fitness Hooks 在同一組，Google Fit／Health Connect 在健康資料組。
- 接著 `shell input swipe 540 1820 540 1170 550`，`85` 確認「安裝工具」與最末下載按鈕完整可見；沒有水平溢位。

### Action: 確認冗餘說明移除 ✅

- `84`／`85` 截圖與階層中沒有先前的 LSPosed、商店管理更新或系統功能補充說明；保留 APP 名稱、簡短描述、版本與按鈕。

### Action: 比對其他頁面的搖桿圖示 ✅

- 從 `85` 點選魔物獵人導航，重新擷取 `86`；`shell input swipe 540 1940 540 1200 550` 後擷取 `87`，確認顯示綠底藍色定位圖示。
- 從 `87` 點選寶可夢導航，重新擷取 `88`；`shell input swipe 540 1950 540 700 550` 後擷取 `89`，確認相同搖桿圖示，與皮克敏 `83` 一致。
- 此階段圖示仍採用已安裝 APP 提供的圖示，未實際測試解除安裝；使用者後續指出回退舊圖問題後，已改為固定打包圖示，以下實機證據不代表後續固定圖示版已安裝。

### Action: 切換返回並檢查穩定性 ✅

- 從 `89` 返回皮克敏，`90` 確認仍在原捲動位置、沒有重複插入分類。
- `logcat -b crash -d -T '10-24 12:09:00.000'` 未發現本 APP 的 FATAL EXCEPTION；APP 程序仍存活。
- 最終顯示參數仍為 1084×2412、420dpi、font scale 1.0；本輪沒有調整這些設定，boot ID 未變。
- 本輪沒有安裝／移除清單中的第三方 APP，也沒有提交或推送 Git。

## 2026-10-24 後續：移除遊戲／定位工具標題

依使用者後續決定，僅保留步數增加工具、健康資料、安裝工具三個分組。`CatalogApp` 的 Pikmin／搖桿沒有分組標題，但仍照原順序建立 APP 項目；不插入空白 heading。移除兩個未使用的字串資源，新增一項純函式測試，確保前兩個 APP 保留且不顯示標題。

### Action: 安裝後開啟皮克敏並檢查兩個標題已移除 ✅

- 197 項測試通過，編譯成功，主來源 lint 0 errors／127 warnings。
- `92` 覆蓋安裝保留資料、冷啟動成功；依當前導航元素執行 `adb -s <device-serial> shell input tap 338 2230`。
- `93-labels-removed-top.json` 與同名 PNG 確認「遊戲／定位工具」標題及其間距消失，Pikmin Bloom、GPS JoyStick、版本與按鈕仍存在，「步數增加工具」標題保留。
- APP 程序存活，boot ID 未變；本輪未改手機顯示設定、未提交或推送 Git。

## 2026-10-24 後續：固定圖示，不依安裝狀態切換

- 原因：先前會從已安裝套件讀 icon，未安裝時則使用舊預設資源，導致皮克敏／搖桿／Fitness Hooks 外觀改變。
- 修正：圖示僅由 `catalogAppIcon(app)` 決定，函式不接受安裝／版本狀態；controller 建立項目時設定一次，`render` 不再設定圖示。三頁搖桿共用更新後的 `gps_icon`。Google Fit／Health Connect 固定 Play 圖示維持不變。
- 移除本次先前加入的 `InstalledAppIcon.kt` 與通用 `catalog_fitness_24.xml`；它們僅是未提交的本輪實作檔案，沒有刪除使用者資料。
- 本機驗證：201 項測試通過，`assembleDebug` 成功，主來源 lint 0 errors／127 warnings，`git diff --check` 通過；Fitness foreground 與來源 XML 核對一致。
- 裝置驗證：預檢回報 `device '<device-serial>' not found`，已通知使用者並停止裝置操作。此固定圖示版尚未安裝／實機驗證，不沿用上一版的實機結果作為本版證據，也沒有移除第三方 APP 來模擬未安裝狀態。
- 所有修改仍未提交／推送。

## 尚需確認

- Android 13 以下 Health Connect 商店／legacy 設定分支尚無相應實機；已做分支與 action policy 檢查。
- 遠端下載站點與商店依裝置／地區可能不同；不代表所有 APK 都已下載或安裝驗證。
- 本次不驗證 Fitness 模組效果、遊戲步數同步、帳號登入或遊戲相容性。
- Nothing 固定圖示版安裝與載入／刷新／跨頁／旋轉驗證已於下述後續測試完成。

## 2026-10-24 後續：固定圖示實機驗證完成

- 使用者確認手機上線後，覆蓋安裝含平板 rail 均分修正的同一份 2.29 APK，已比對裝置端 SHA-256 與本機一致。
- 目前 user 0 未安裝的 Pikmin 與 APKMirror Installer，在載入、刷新、跨頁返回及畫面重建後均保留固定圖示。Pikmin 安裝在 user 10，不影響目前 user 0 的「未安裝」判定。
- 三個遊戲頁的搖桿圖片一致，Fitness／Google Fit／Health Connect 圖示完整。沒有為測試而移除第三方 APP。
- 原始解析度／密度／字體／自動旋轉均已還原；未提交或推送。工具限制、過程中的一次版本查詢失敗與完整證據見 [本輪實機紀錄](rail-icons-device-validation-20261024.md)。
