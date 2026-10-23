# 遊戲標題與首頁卡片尺寸還原

日期：2026-10-14。

## Git 與範圍

- 先將既有 MHN 相容性摘要／雙通道檢測提交為 `6974720`，作者 email 為 GitHub noreply，未推送遠端。
- 本文件記錄的尺寸還原保留為未提交變更；原有 `app/build.gradle` 版本修改、Pokemod 測試與本機工具目錄未納入上述提交。
- 沿用 Kotlin／XML／Fragment／Material；沒有改動導航、版本選擇、檢測、下載、安裝與更新邏輯，沒有新增依賴。

## 歷史基準與修改

- `0db0313` 曾縮小遊戲頁標題。本次還原 Pokémon／MHN 直向展開標題至 HeadlineMedium（28sp），直向及橫向收合標題至 TitleLarge（22sp）。
- 橫向展開標題維持 C2 的 HeadlineSmall（24sp），保留多行副標題及自適應工具列高度。
- `a446322` 曾在首頁直向卡片增加每側 8dp 外距。本次單欄直向移除此額外外距，保留容器原有 16dp padding；圓角、內容 padding、文字大小與 wrap_content 高度不變。
- 橫向及雙欄仍保留每側 8dp 外距；欄數計算門檻不變，避免兩張卡片貼在一起或寬度切換反覆重排。

修改檔案：

- `app/src/main/res/values/styles.xml`
- `app/src/main/res/layout/fragment_home.xml`
- `app/src/main/java/app/lokey0905/location/fragment/Home.kt`
- `app/src/main/java/app/lokey0905/location/ui/layout/ResponsiveGridPolicy.kt`
- `app/src/test/java/app/lokey0905/location/ui/layout/ResponsiveGridPolicyTest.kt`
- 本驗證文件。

## 編譯與測試

- `:app:testDebugUnitTest :app:assembleDebug :app:lintReportDebug --offline --max-workers=2` 成功；使用 JDK 21、Gradle JVM 2GB。
- 單元測試 163 項，0 失敗、0 錯誤、0 跳過。新增 4 項涵蓋直向還原外距、雙欄門檻、橫向外距及重建／尺寸切換。
- 主程式 lint：0 errors、133 warnings。沿用既有執行方式，排除 `lintAnalyzeDebugUnitTest` 與 `lintAnalyzeDebugAndroidTest`，不宣稱測試來源 lint 已通過。
- `git diff --check` 通過。另經唯讀檢查，重排判斷同時比對欄數與外距，不會修改廣告 banner 的外距或改變原本欄數門檻。
- Debug APK SHA-256：`773E0A3E0712CD78876C9F25C76ABAB8C339C75D5B58437729A9872C890A762C`。

## Nothing A142 實機

- 覆蓋安裝 2.29，保留資料；Android 16，1084×2412，420dpi。
- 首頁裝置卡片由 `[63,588][1021,1512]` 變為 `[42,567][1042,1491]`：寬度 958→1000px，回復 16dp，內容高度仍為 924px。
- 一般字體：兩個遊戲頁直向展開／收合正常；首頁橫向雙欄仍有 42px 卡片間距；MHN 橫向收合標題正常。
- 200% 字體：兩個遊戲頁展開／收合標題及副標題可辨識，未觀察到重疊或水平溢位。長工具名稱可換行。
- 已還原字體 1.0、自由旋轉；尺寸、密度與 boot ID 均與測試前一致。未重啟、未清資料、未操作第三方 APP 安裝或移除。
- 本次程序的 crash buffer 無紀錄，保留的 crash buffer 中也沒有本 APP 的 Process crash 紀錄。
- 截圖與 UI hierarchy 存於忽略追蹤的 `app/build/ui-size-restore-20261014/`，包含前後對照及最終還原畫面。

## 未驗證範圍

- 本次未重測實體平板／MIUI、360dp 窄視窗或 release build；沒有撤回先前的自適應布局。
- 下載、更新與移除流程本次沒有實際觸發，相關程式碼未更動。

## 後續：小標題字級還原

- 依使用者追加要求，比對 `0db0313^` 與本機 Material 1.13.0 資源，在 `styles.xml` 恢復兩個遊戲頁副標題：直向展開 HeadlineSmall（24sp）、橫向展開 TitleLarge（22sp）、收合 TitleMedium（16sp）。保留多行與動態展開高度。
- 只還原字級時，Nothing 的 200% 字體收合副標題下緣會被裁切；`game_readability.xml` 的共用收合容器由 64sp 增至 80sp，72sp 中間版本仍不足。一般字體收合區因此增加 16dp，不縮小已還原字級，也沒有新增 Kotlin 邏輯。
- 最終 80sp 版已覆蓋安裝。Nothing Android 16、200% 字體的兩頁收合副標題均無垂直裁切；較長的 MHN 狀態收合時正常顯示省略號，展開兩行可閱讀。一般字體 MHN 橫向展開／收合正常。
- 沿用相同 Gradle 驗證指令成功；163 項單元測試結果無失敗（此次資源修改命中測試快取），主程式 lint 0 errors／133 warnings，`git diff --check` 通過。
- 最終 APK SHA-256：`87640DC4273F4C38598C0231BD6CAE664B06D411BD6ABC83C7D016312E5D44E2`。前述 SHA-256 為本次小標題還原前的版本。
- 手機字體 1.0、自由旋轉、尺寸／密度／boot ID 均已比對還原；本 APP 無留存 crash 紀錄。證據為同目錄 `23-subtitle-before` 至 `47-subtitle-final-home`，其中 `38`／`40` 是最終 200% 收合驗證，`43`／`44` 是最終橫向驗證。
- 本次僅追加修改 `styles.xml`、`game_readability.xml` 與本文件；保留既有首頁／大標題修改及使用者的 `version_labels.xml` 變更。沒有 Git 提交或推送。
- 未測實體平板／其他 OEM／超過 200% 字體，不將 Nothing 的結果推廣到這些環境。
