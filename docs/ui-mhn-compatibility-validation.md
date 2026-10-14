# MHN 相容性摘要與雙通道檢測驗證

日期：2026-10-14。沿用 Kotlin／XML／Fragment／Material，未新增依賴、未提交或推送 Git；基準 HEAD 為 `e85cc9b`。

## 最終行為

- MHN 上方改成與 Pokémon 相同的目標版本選擇器、相容性列及預設收合的完整支援清單。
- 正式版與測試版同時查詢、各自顯示支援結果；任一來源失敗不覆蓋另一來源。
- 下方只有一張 MHNTools APP 卡片。保留設定頁的 `mhntools_testversion` 開關，移除遊戲頁重複開關，與 PGTools 現有方式一致。
- 切換通道僅套用已取得的版本／下載 URL，不重查網路；兩版使用相同套件名稱，不能藉相同 versionName 推斷安裝通道。
- 未安裝 APP 隱藏右側選單；已安裝 APP 保留選單、移除及開啟／更新按鈕。
- 重用共享 `getCompatibilityStatus` 與數字版本比較；null 為尚未確認、已知空列表為不支援、找不到目標時顯示最高支援版本。數字尾碼政策沿用 Pokémon。
- 合併兩個通道的遊戲版本後排序，仍只使用 `mhnVersion` 作為選版狀態。刷新開始清除舊下拉項目，兩個來源完成前不因局部回應丟失目前選擇。
- 檢測具取消／回應世代檢查、連線與讀取 timeout、finally disconnect；已安裝 MHNTools 在遠端查詢中／失敗時仍可開啟。

## 修改檔案

- `app/src/main/java/app/lokey0905/location/fragment/AppsMHN.kt`
- `app/src/main/java/app/lokey0905/location/api/MhnToolsVersionParser.kt`（新增）
- `app/src/main/java/app/lokey0905/location/version/MhnVersionSelection.kt`
- `app/src/main/res/layout/fragment_apps_mhn.xml`
- `app/src/main/res/layout/fragment_apps_poke.xml`（選單初始狀態改為 gone）
- `app/src/main/res/values/mhn_selector.xml`
- `app/src/test/java/app/lokey0905/location/api/MhnToolsVersionParserTest.kt`（新增）
- `app/src/test/java/app/lokey0905/location/version/MhnCompatibilityTest.kt`（新增）
- `app/src/test/java/app/lokey0905/location/version/MhnVersionSelectionTest.kt`
- 本紀錄。

設定頁程式／XML 已保留原內容。原有 `app/build.gradle` 版本變更、未追蹤 Pokemod 測試及本機工具資料夾不屬於本次修改，未暫存任何檔案。

## 標題大小核對

先前 D2 提交 `0db0313` 將直向展開標題由 Material HeadlineMedium 28sp 改為 HeadlineSmall 24sp；收合標題由 TitleLarge 22sp 改為 TitleMedium 16sp。橫向展開標題原本已為 24sp。這次僅核對，未更動標題樣式。

## 編譯與單元測試

JDK 21.0.12.1、既有 Gradle wrapper，執行：

```text
:app:testDebugUnitTest :app:assembleDebug :app:lintReportDebug
-x :app:lintAnalyzeDebugUnitTest -x :app:lintAnalyzeDebugAndroidTest
--offline --console=plain --max-workers=2
-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

- 最終 BUILD SUCCESSFUL。
- 159 項測試：0 failures、0 errors、0 skipped。包含原有未追蹤的 5 項 Pokemod 測試。
- 新增 23 項：13 項雙通道 JSON／URL／排序／缺資料測試、6 項共享相容性整合測試、4 項局部回應選版測試。
- 主程式 lint：0 errors、133 warnings；不是零警告。包含版面深度／View 數量提示，未為消除提示重構既有 APP 清單。
- 測試來源 lint 因既有本機逆向分析產物問題排除，不能稱為全來源 lint。
- debug 2.29 APK SHA-256：`194522C9ED0A4CDA6AB95923317C0E61531D01E391EBE1D9FAF6D0D6CA185A25`。
- `git diff --check` 通過。

## Journey：Nothing 實機回歸

Nothing A142／Android 16，使用既有 serial／boot guard 的 ADB helper，依 android-cli 互動指引逐步讀取 hierarchy，截圖後立即檢視。證據位於本機 `app/build/mhn-ui-20261014/`，不提交二進位證據。

### 覆蓋安裝並開啟 ✅

- `adb install -r app-debug.apk`、`adb shell am start -n app.lokey0905.location/.MainActivity`。
- 安裝成功，保留資料。第一次 UI dump 尚未就緒，重新讀取取得正常定位提示；僅點取消，未變更定位設定。
- `14-dual-start.xml`、`15-dual-mhn.xml/png`：130.0、兩列支援、完整清單收合。

### 展開與切換目標版本 ✅

- 點 `mhn_toggle_all_supported_versions`，點 `mhn_spinner`，依截圖選 129.1。
- `16-dual-expanded.xml/png`：兩份完整支援列表與向上箭頭正常。
- `17-dual-dropdown.png`、`18-dual-selected.xml`：版本選擇與下方目標同步，兩列支援狀態正常。

### 旋轉與捲動 ✅

- `adb shell wm user-rotation lock 1`，以 700ms swipe 捲動內容。
- `19-dual-land-activity.txt` 確認實際 919×413dp 橫向；`20`／`21`／`22` 證明 129.1 與展開狀態保留，兩份列表能捲動讀取。
- 清單超出可視高度時需捲動，不將「暫時在畫面外」判為狀態遺失。

### 設定切換正式版／測試版 ✅

- 設定頁原值為測試版 true；先切 false，再回 MHN；最後切回 true。
- `26`／`27`／`28`：正式版卡片不帶測試版標籤；`32`／`33`／`34`：原設定已恢復，單張卡片顯示測試版。
- 切換期間查詢起始記錄維持兩筆，未重新發出版本請求。
- 點 MHNTools「下載」會開既有確認框（`29`），已取消（`30`）；未開下載網址或安裝工具 APK。

### 選單與其他卡片回歸 ✅

- `34-tool-cards.xml/png`：未安裝 MH Now／MHNTools／hylianer 不顯示選單，已安裝 GPS 仍有選單、移除與開啟。
- `35`／`36`／`37`：Pokémon／三星版既有更新與移除按鈕保留；未安裝 PokeList／WeCatch 選單隱藏，已安裝 DeFit 選單保留。
- 未執行其他 APP 的更新、移除或安裝；這些入口的呈現與原 action code 已核對，並非完整安裝流程實測。

### 下拉刷新與還原 ✅

- 恢復目標 130.0、收合清單，再下拉刷新。
- 首輪正式／測試請求同為 21:02:29.471；刷新輪為 21:10:11.805／806，均為並行啟動。
- 最後 log buffer 已輪替，因此累計四筆的測試斷言不成立；不將該計數當成 APP 失敗或效能數據。`44-mhn-check-log.txt` 保留刷新輪兩個來源的記錄，`44-final-mhn.xml` 確認目標保留且兩列支援。
- `45-final-baseline.json` 與測試前一致：1084×2412、420dpi、自由旋轉、fontScale 1.0；沒有重啟、清資料或更動不鎖定設定。
- 本次程序 PID 9063 持續存在，該程序 crash buffer 空白。其他 APP 的既有 crash 不算本 APP 問題。

## 未驗證範圍

- MHNTools 未安裝於測試手機；已安裝但遠端失敗的主按鈕保留由程式複核確認，未為此安裝第三方工具或注入網路故障。
- 空列表、缺資料、不同支援版本、單來源失敗、數百版本由純函式測試覆蓋；當前真實 API 兩版均為 130.0／129.1，未偽造實機 API。
- 長清單的 XML 使用 wrap_content 與 NestedScrollView；這次未在實機注入數百筆，也未重新測試 200% 字體、MIUI、真正平板或多視窗。
- 未驗證 release 混淆／最佳化產物。Pikmin 尚未納入。
