# 平板左側導覽均分高度（2026-10-24）

## 範圍與實作

- 沿用 XML／Fragment 與 Material 1.13.0 NavigationRailView，不遷移 Compose、不新增依賴。
- 僅套用既有 `sw600dp-w840dp-land` 平板橫向布局。手機底部導航、五個目的地順序、頁面切換／返回邏輯均不變。
- rail 原本已滿高，但選單靠上且項目使用自然高度。本次移除元件額外的頂部內容間距，將項目圖示／文字置於各格中央。
- 可用高度 = rail 實際高度 − rail 頂部／底部 padding；不重複扣除外層已處理的底部系統安全區。
- 項目 minimum = max(原始 minimum 或 rail 寬度, 可用高度 / 可見項目數)。五項除法餘數最多 4px，留在底部。
- 每次尺寸或安全區改變後重新計算；保留原始 minimum，使縮窗能縮回，不沿用之前拉伸的值。同值不重新要求 layout。
- 保留 `scrollingEnabled`，不操作 Material 內部 View、不使用反射、不強制固定子項目高度。圖示、字型及選取效果的尺寸維持原設定。

## 驗證範圍

新增 9 項純函式測試：五項均分、安全區輸入、除法餘數、短視窗 minimum、雙向縮放、不同項目數、無有效高度、空選單及無效 minimum。

短視窗測試驗證傳入元件的 minimum，並非實機捲動證明。Material 內部仍會限制 minimum 至可分配高度，但保留項目的自然內容高度；內容超出視窗時才由既有 scroller 處理。短視窗／大字體的實際觸控、捲動與視覺位置仍需實機驗證。

### 本機驗證

以下為含遊戲頁面更新的整合工作樹驗證紀錄；平板程式修正可獨立提交，但不將此 APK 當成僅有平板修正之提交的獨立實測證據。

執行既有 Gradle wrapper：

```text
:app:testDebugUnitTest :app:assembleDebug :app:lintReportDebug
-x :app:lintAnalyzeDebugUnitTest -x :app:lintAnalyzeDebugAndroidTest
--offline --console=plain --max-workers=2
```

- 編譯成功；210 項單元測試，0 failures／errors／skipped（本次新增 9 項）。
- 主來源 Lint：0 errors／127 warnings，與修改前數量一致；本次新增的兩項 KTX 提醒已修正後重跑。
- `git diff --check` 通過；沒有 staged 檔案。
- 測試 APK SHA-256：`440F7627DF53E4B50D432B4AAD27A590DF9EBEBCE304B82D0599A74BF765D864`。

### 實機驗證：已完成手機模擬平板測試

使用者確認 Nothing A142 恢復上線後，已覆蓋安裝相同雜湊 APK。2400×1600／240dpi 時五項皆為 305px；2200×1440／360dpi／兩倍字體時皆為 266px。五個目的地與系統返回正常，原始顯示設定已完整還原，手機原尺寸仍使用底部導航。

未安裝的 Pikmin／APKMirror Installer 固定圖示也通過刷新、跨頁及重建後檢查。詳見 [本輪實機紀錄](rail-icons-device-validation-20261024.md)。仍未在實體平板驗證，也未觸發極短視窗的 rail 溢出捲動分支。

## 核對來源

- [Material 1.13.0 NavigationRailView](https://github.com/material-components/material-components-android/blob/1.13.0/lib/java/com/google/android/material/navigationrail/NavigationRailView.java)
- [Material 1.13.0 NavigationRailMenuView](https://github.com/material-components/material-components-android/blob/1.13.0/lib/java/com/google/android/material/navigationrail/NavigationRailMenuView.java)
- [Material 1.13.0 NavigationBarView](https://github.com/material-components/material-components-android/blob/1.13.0/lib/java/com/google/android/material/navigation/NavigationBarView.java)

驗證階段沒有提交或推送 Git；提交依使用者後續要求分開處理。
