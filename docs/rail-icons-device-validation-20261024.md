# Journey: 平板導覽間距與固定 APP 圖示（2026-10-24）

## 結果

Nothing A142／Android 16 實機覆蓋安裝通過，並用暫時的顯示尺寸／密度模擬平板橫向。兩種尺寸的五個左側導覽項目均等高；未安裝的 Pikmin 與 APKMirror Installer 圖示在刷新、跨頁及畫面重建後維持正確。沒有移除第三方 APP，也沒有修改產品程式碼、提交或推送 Git。

這不是 Xiaomi Pad 6 真實平板實測，也不包含 MIUI 桌面捷徑圖示問題。

## 裝置與基準

- 已固定核對同一裝置序號，目前 Android user 0；識別值只保留在本機忽略的測試證據。
- boot ID 全程相同，具體值不納入版本控制。
- APK：2.29 debug，SHA-256 `440F7627DF53E4B50D432B4AAD27A590DF9EBEBCE304B82D0599A74BF765D864`。
- 證據目錄：`app/build/rail-icons-20261024/`；原始設定記於 `device-baseline.json`。
- 原始設定：1084×2412，420dpi，沒有尺寸／密度 override，font_scale=1.0、accelerometer_rotation=1、user_rotation=1。
- 安裝後主程序 PID 為 27647，最後確認仍為相同 PID。Android exit-info 最新主程序退出原因為這次覆蓋安裝造成的 `PACKAGE UPDATED`，沒有後續主程序退出紀錄。

## 驗證紀錄

### Action: 覆蓋安裝、保留資料並啟動 ✅

- `android run --device=<device-serial> --apks=<app-debug.apk> --activity=app.lokey0905.location.MainActivity --install-options=-r --use-delta-install=false`。
- `pm path` 搭配裝置端 `sha256sum` 確認已安裝 base.apk 與本機 APK 完全相同。
- 首次使用 component 格式的 activity 參數未通過 CLI 檢查，改用完整類別名稱後成功；不曾解除安裝或清資料。
- `01`～`03` 截圖確認既有首頁、取消定位提示及皮克敏畫面。僅取消提示，沒有變更定位設定或勾選「不再顯示」。

### Action: 確認目前使用者未安裝的 APP 與刷新後圖示 ✅

- `pm list packages --user 0` 中沒有 Pikmin／APKMirror Installer。
- Pikmin 的 `dumpsys package` 明確顯示 user 0 `installed=false`、user 10 `installed=true`；APP 顯示「未安裝」符合目前使用者狀態，不是偵測錯誤。
- [03 手機初始畫面](../app/build/rail-icons-20261024/03-pikmin-phone-top.png) 與 [04 下拉刷新後](../app/build/rail-icons-20261024/04-pikmin-phone-refreshed.png)：Pikmin 維持綠色花朵／紅色皮克敏圖示，沒有回退成通用圖示。
- 下拉操作：`input swipe 540 650 540 1400 600`。

### Action: 模擬平板橫向並量測五項間距 ✅

- 暫時設定：`wm size 1600x2400`、`wm density 240`、`settings put system accelerometer_rotation 0`、`settings put system user_rotation 1`。
- 穩定後畫面為 2400×1600、sw1067dp／w1600dp／h1067dp。
- [07 平板左側導覽](../app/build/rail-icons-20261024/07-tablet-rail-top.png) 及 `dumpsys activity top` 的 View bounds：

| 導覽項目 | Menu 內 Y 範圍 | 高度 |
| --- | --- | --- |
| 寶可夢 | 0–305 | 305px |
| 皮克敏 | 305–610 | 305px |
| 首頁 | 610–915 | 305px |
| 魔物獵人 | 915–1220 | 305px |
| 捷徑 | 1220–1525 | 305px |

- rail 寬 120px、外層高 1564px，內容容器高 1528px，選單高 1525px；剩餘 3px 在底部。各項圖示／文字位置一致，沒有貼頂或遮住手勢區。

### Action: 捲到清單底部並核對圖示 ✅

- `input swipe 1400 1400 1400 650 600`。
- [08 清單底部](../app/build/rail-icons-20261024/08-tablet-pikmin-bottom.png) 確認未安裝的 APKMirror Installer 仍是橘底白色 M，Google Fit／Health Connect 使用指定圖示，Fitness 前景完整。
- 七個 catalog 圖示皆已在本輪畫面查看。未為測試而安裝、移除或開啟第三方 APP。

### Action: 五個導覽項目與系統返回 ✅

- 依目前 rail bounds 點擊：MHN `(185,1103)`、寶可夢 `(185,188)`、捷徑 `(185,1408)`、首頁 `(185,798)`、皮克敏 `(185,493)`。
- `09`～`13` 每一步都有截圖確認選取狀態與目的頁。Pogo／MHN／Pikmin 的搖桿圖示一致。
- [13 返回皮克敏](../app/build/rail-icons-20261024/13-tablet-pikmin-return.png) 保留捲動位置，APKMirror Installer 等圖示未改變。
- 從皮克敏執行一次 `input keyevent 4`；View hierarchy 顯示 `nav_home` selected，仍在同一 MainActivity／PID，沒有退出 APP。

### Action: 較小橫向尺寸與兩倍字體 ✅

- 暫時設定 `wm size 1440x2200`、`wm density 360`、`settings put system font_scale 2.0`。
- 穩定畫面為 2200×1440、sw640dp／w978dp／h640dp。
- `15` 的 View bounds 顯示五項分別為 `0–266`、`266–532`、`532–798`、`798–1064`、`1064–1330`，每項 266px，已正確縮小而非保留前一次 305px。
- [16 大字體畫面](../app/build/rail-icons-20261024/16-compact-rail-clear.png)：五個標籤均可讀，沒有水平溢位；此尺寸仍容得下五項，因此沒有宣稱實測到 rail 溢出捲動分支。

### Action: 還原原始顯示設定並確認手機底部導航 ✅

- `wm size reset`、`wm density reset`、font_scale=1.0、user_rotation=1、accelerometer_rotation=1。
- 最終逐欄讀回與基準完全相同；`wm user-rotation` 為 `free`，沒有顯示 override。
- [19 還原後手機橫向](../app/build/rail-icons-20261024/19-phone-cancelled.png) 顯示原本底部導航，不再使用平板 rail；自動旋轉已恢復，當下畫面仍為橫向。
- boot ID 未變、ADB 未中斷；沒有重啟、清資料或移除第三方 APP。

## 工具限制與其他觀察

- 按 android-cli 技能先讀取 layout；本輪 instrumentation server 回覆 `Unrecognized response`，內建 UIAutomator dump 也未成功。改以每次操作後立即查看的 screenshot，以及 `dumpsys activity top` 實際 View bounds 交叉驗證。沒有重設 ADB server 或安裝新的 UI 測試工具。
- `logcat -b crash -d --pid=27647` 未返回，已中止讀取；不把空輸出當成乾淨的 crash log。穩定性結論限於實際 UI 操作、相同 PID 持續存活，以及沒有後續 process exit 的證據。
- 一次畫面重建後 Fitness 版本查詢失敗；圖示仍保持正確。後續還原尺寸、重新查詢時顯示「版本查詢完成」，未針對遠端查詢另作修正或認定根因。
- 本輪沒有重新編譯；安裝的是上一輪已通過 210 項單元測試、assembleDebug 與主來源 Lint（0 errors／127 warnings）的同一份 APK，並驗證其安裝後雜湊。
- 實機驗證結束時 Git HEAD 為 `6fb0998`，該輪沒有 staged 檔案、commit 或 push；後續提交另依使用者指示處理。
