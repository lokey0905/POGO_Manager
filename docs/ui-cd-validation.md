# C／D UI 改善驗證紀錄

日期：2026-10-13。沿用 XML／Fragment／ViewBinding；未遷移 Compose、重做導覽架構或新增第三方依賴。每個小項獨立提交本地 master，沒有推送 GitHub。

## 修改項目與主要檔案

| 分項 | 行為 | 主要檔案 |
| --- | --- | --- |
| C1 | 平板橫向寬度達門檻改用左側五格導覽；手機／窄視窗保留底部 | `MainActivity.kt`、`layout/activity_main.xml`、`layout-sw600dp-w840dp-land/activity_main.xml` |
| C2a／C2b | 兩個遊戲頁橫向卡片最大 840dp，置中留白與較低標題；仍為單欄 | `fragment_apps_poke.xml`、`fragment_apps_mhn.xml`、`values[-land]/dimens.xml`、`values[-land]/styles.xml` |
| C2c（新增要求） | MHN 改成與 Pokémon GO 相同的 Material 外框下拉選擇器，標籤「目標 MH Now 版本」 | `AppsMHN.kt`、`fragment_apps_mhn.xml`、`mhn_selector.xml`、`MhnVersionSelection.kt` 與測試 |
| C3a | 首頁按實際寬度／字體選擇 1–2 欄，卡片自然增高 | `Home.kt`、兩份 `fragment_home.xml`、`home_layout.xml` |
| C3b | 捷徑靜態／遠端卡片按寬度／字體選擇 1–4 欄，文字可換行 | `ShortCuts.kt`、`fragment_shortcuts.xml`、`shortcut_layout.xml`；共用 `ResponsiveGridPolicy.kt` 與測試 |
| D1 | 清楚區分 APP 最新／已安裝／目標下載版本；查詢中、失敗、未知、舊資料及 Polygon 後續查詢的顯示 | `AppsPoke.kt`、`AppsMHN.kt`、兩份遊戲 XML、`version_labels.xml`、`VersionCheckPresentation.kt` 與測試 |
| D2 | 相容性名稱／狀態保留寬度並換行；遊戲標題／副標題多行及隨字體增加收合高度 | `item_pogo_compatibility_status.xml`、`values[-land]/styles.xml`、`game_readability.xml` |
| D3 | 一般捷徑 vector 先以非零 bounds、Material 顏色繪成 bitmap，避免 MIUI 缺少 bounds 的轉換路徑 | `ShortCuts.kt`、`shortcut/PinnedShortcutIcon.kt`、`ShortcutIconGeometry.kt` 與測試 |

Kotlin 路徑皆位於 `app/src/main/java/app/lokey0905/location/`，XML 位於 `app/src/main/res/`，測試位於 `app/src/test/java/app/lokey0905/location/`。

MHN 仍以原本 `mhnVersion` 為唯一選擇狀態，保留來源排序、fallback、測試版切換、saved state 與下載 URL。D1 僅修改呈現，沒有變更下載／更新／移除／開啟 APP 流程或網路刷新排程。D3 保留捷徑 ID、名稱、Intent、URL、釘選流程；定位精確度小工具不變。

## 編譯與單元測試

以 JDK 21.0.12.1 執行 Gradle wrapper：

```text
:app:testDebugUnitTest :app:assembleDebug :app:lintReportDebug
-x :app:lintAnalyzeDebugUnitTest -x :app:lintAnalyzeDebugAndroidTest
--offline --console=plain --max-workers=2
-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

- 最終 BUILD SUCCESSFUL。
- 136 項單元測試：0 failures、0 errors。包含既有未追蹤的 5 項 Pokemod 測試，未將該檔混入此次提交。
- 本次增加：15 項響應式欄數、14 項版本呈現、4 項 MHN 選擇、8 項捷徑幾何測試。
- 主程式 Lint：0 錯誤、130 警告；未將它描述為零警告。測試來源 Lint 因本機 build 下的逆向分析產物問題排除，並非全來源 Lint 驗證。
- 安裝的是 debug 2.29 測試 APK；未驗證 release 最佳化／混淆產物。
- APK SHA-256：`A4311388E713204B537D26216B7B6B64E1F9E622A639F74026BF7D5001874F10`。

## Nothing 實機驗證

Nothing A142、Android 16，僅操作獲授權的 Nothing。覆蓋安裝保留資料，全程無重啟／清資料，開機識別未變。模擬用尺寸／密度／旋轉／字體已還原至原本 1084×2412、420dpi、free、1.0；沒有改回使用者自行設定的「不鎖定」選項。

| 項目 | 結果／證據（本機 `app/build/cd-20261013/`，不提交影像） |
| --- | --- |
| C1 導覽 | 一般底列、模擬 1280×800dp rail、839／840dp 邊界、返回與還原通過；`c1-report.md` |
| 兩遊戲橫向內容 | Pokémon `20-c2a-tablet.png`、展開 `21-c2a-expanded.png`；MHN `30-c2b-land.png` 與 activity config 驗證真正橫向 |
| 首頁／捷徑 | 一般手機、模擬平板、360dp 窄窗、200% 字體通過；`31`／`32`、`35`／`34`、`39`／`40`、`46`／`47` 截圖 |
| MHN 選擇器 | 下拉順序、選 129.0、旋轉保持、測試版切換通過；`54`／`57`、`58`／`59`、`62`；`64` 與最終 `94-final-mhn-loaded.xml` 確認還原 129.1 正式版 |
| 大字體 | 360dp／200% 下 MHN 選擇器、相容性列、收合標題與展開清單可用；`68`–`71` 截圖 |
| D1 | 版本標籤與刷新回歸通過；loading／failure／Polygon 後續查詢的分支由純函式測試涵蓋，未注入真實網路故障或安裝另一版 Polygon |
| 最終回歸 | `92-final-mhn.xml` → 返回 → `93-final-back.xml` 仍為捷徑 Fragment，未退回桌面；`94` MHN 資料載入完成 |
| 還原 | `73-d3-start.json` 與 `90-final-restored.json` 一致；崩潰 buffer 未發現此 APP 的 crash，不將其他 APP 的既有錯誤算為本 APP 問題 |

### D3 桌面捷徑旅程

1. 長按「位置冷卻計算機」→ 系統釘選預覽：通過，完整圖示（`76-d3-pin-cool.png`）。
2. 新增至主畫面：通過。
3. 長按「使用手冊」→ 系統釘選預覽：通過，完整圖示（`79-d3-pin-manual.png`）。
4. 新增至主畫面，檢查兩個圖示非空白、完整可辨識：通過（`80-d3-desktop.png`）。
5. 點擊冷卻捷徑：Chrome 網址為 `cool.android.ovh`（`81-d3-cool-target.xml`）。Chrome 同時存在先前的 Polygon 下載確認，本次未點擊／取消／批准該下載，未將網站內容載入視為已驗證。
6. 點擊手冊捷徑：Chrome 網址為 `hackmd.io/@lokey0905/POGOManager`（`83-d3-manual-target.xml`）；程式仍傳入原有 `B1x_pZAT2` 資源 URL，未更動連結。
7. 只從桌面「移除」本次建立的兩項測試捷徑：通過。待移除提示消失後，系統 dump 不再列出這兩個 pinned ID；未卸載 APP／刪除其他圖示。可由 APP 重新長按建立。

操作以 serial／boot guard 包装的 `adb shell input` 執行；每一步讀取 UI hierarchy，必要時截圖並實際檢視。移除後第一次 hierarchy 只有暫時的「已移除項目」提示，因此未找到下一個圖示；沒有盲點座標，待桌面穩定後重新定位完成清理。

## 尚待驗收與既有限制

- **Pad 6／Redmi 不在線：D3 特定 MIUI 修復效果尚未實測。** 已有相應桌面 Java／smali 分析作為修正依據，但 Nothing 通過不能替代 MIUI 驗收。請同版 APK 重建兩個捷徑並確認完整圖示、正確目標。
- 舊的空白捷徑未驗證自動更新；先刪除該桌面捷徑，再由 APP 長按重建。未實作主動更新所有既有 pinned shortcuts。
- 真正平板、多視窗、RTL、三鍵導航與深色桌面圖示仍需額外實機確認；已測的平板／窄窗為 Nothing 模擬尺寸。
- 200% 字體下 Pokémon Material 浮動標籤可能省略尾端；版本值、工具名稱／狀態與展開內容仍可讀。
- MHN 既有 API 部分例外缺少終止回呼，可能持續顯示查詢中；本輪未擴大修改 API。Polygon 先失敗後成功時，既有重試 Snackbar 也可能尚未消失，但版本／標題已恢復。
- 未實際下載／更新／移除其他 APP；既有 action code 未變，按鈕呈現已檢查。遠端所有下載卡片／廣告載入未逐項實測；原 320dp 固定 Banner 策略未改。
- 未混入 `app/build.gradle` 的原有 2.29 版本變更、未追蹤 Pokemod 測試與 `.codex-remote-attachments/`、`.serena/`、`.vscode/`。

`adaptive` 技能提供尺寸／字體自適應原則；因本案明確要求保留 XML／Fragment，沒有採用其 Compose／Navigation 3 遷移步驟。裝置驗證依 `android-cli` 互動／旅程指引逐步讀取畫面，使用既有序號鎖定 ADB 執行。
