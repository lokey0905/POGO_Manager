# Pad 6 導覽與一般捷徑驗證

日期：2026-11-05。此紀錄補充 [C／D 驗證紀錄](ui-cd-validation.md)，不改寫先前測試結果。

## 範圍與環境

- 使用者確認：平板橫向左側五個導覽項目改為靠上排列，不加入展開／收合按鈕。
- 保留 XML／Fragment、頁面順序、返回及安全區域處理；手機／窄視窗仍使用底部導覽。
- 移除自行平均分配導覽項目高度的函式、布局監聽及其 9 項測試；由 Material 元件決定自然高度。沒有修改捷徑實作、下載或版本檢測邏輯。
- 實機：Xiaomi Pad 6（23043RP34G），Android 13／API 33；桌面 `RELEASE-4.40.0.5839-11141649`。
- 螢幕原值：1800 × 2880、400 dpi、字體倍率 1.0、自動旋轉開啟、`user_rotation=0`。
- APK：debug 2.29／versionCode 3，SHA-256 `A0AD5ED65D4AA0A00DB3C1539F52EBEAFD91BDBD91CB1501E121FD21B6DDC72C`。
- 已核對新舊 APK 簽署憑證相同，備份原 APK 後使用 `adb install -r` 覆蓋安裝，保留資料。

## 編譯與單元測試

使用現存 JetBrains JDK 21 執行：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline --console=plain --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx2048m" "-Dfile.encoding=UTF-8"
```

- 修改前：210 項測試通過。
- 修改後：201 項測試通過，0 failures／errors／skipped；包含 8 項捷徑圖示幾何測試。
- 上述為測試當時工作目錄的總數，均包含既有未追蹤的 `PokemodVersionParserTest` 5 項；該檔不納入本次提交。
- `assembleDebug` 成功；本次未執行 lint，不宣稱完整 lint 通過。

## Journey 1：導覽與畫面重建

1. 開啟首頁，比對修改前後相同橫向尺寸的 UI hierarchy 與截圖。
   - 修改前每格 340 px，五格佔據 y=60–1760。
   - 修改後每格 165 px（此機設定下約 66 dp），五格集中 y=60–885；這是實測尺寸，不是程式固定值。
   - 五個圖示、文字與選取標記完整，頂部避開狀態列；沒有新增展開按鈕。
2. 依序切換寶可夢、皮克敏、魔物獵人、捷徑，逐次檢查選取標記與頁面標題：通過。首頁已於啟動時驗證。
3. 捷徑頁按系統返回：回到上一個魔物獵人頁，沒有離開 APP。
4. 暫時鎖定直向，再切回橫向；待旋轉動畫結束後檢查 hierarchy。
   - 直向使用底部五格，仍選中捷徑。
   - 橫向恢復左側靠上五格，仍選中捷徑。
   - 最後還原自動旋轉與原 `user_rotation` 值。

結果：通過。未在本輪額外模擬窄視窗、大字體或其他裝置。

## Journey 2：MIUI 一般捷徑圖示

1. 建立前使用 `cmd shortcut get-shortcuts --user 0 --flags 15 app.lokey0905.location` 檢查，沒有該 APP 的既有捷徑紀錄。
2. 長按「位置冷卻計算機」，MIUI 直接新增桌面捷徑。
3. 將「使用手冊」捲動至完整可見，再長按新增。
4. 系統紀錄顯示 `coolDownCalculator`、`manual` 為 pinned shortcut，圖示儲存為 bitmap，而非 drawable resource ID。
5. 查看桌面截圖：計算機與書本圖案皆完整可辨識，沒有空白或裁切；鎖定後重新喚醒也確認兩者正常。
6. 分別點擊桌面捷徑，透過瀏覽器網址列確認：
   - 計算機：`cool.android.ovh`。
   - 使用手冊：`hackmd.io/@lokey0905/POGOManager`。
7. 透過桌面選單移除本次新建的兩個測試捷徑；再次查詢沒有該 APP 的捷徑紀錄，其他桌面項目未操作。

結果：Pad 6 桌面圖示及開啟目標通過。本次 MIUI 沒有提供可停留檢查的釘選預覽，不能記作預覽驗證通過。

## 邊界與復原

- 沒有重啟、清除資料、改動 LSPosed／桌面設定，或安裝／移除頁面列出的第三方 APP。測試期間 boot ID 不變。
- 測試工具使用了 Android CLI 的 layout instrumentation；不修改應用程式的功能設定。
- 尺寸、密度、字體及旋轉設定已確認與原值相同。依使用者允許，以 ADB 喚醒並關閉無密碼鎖定畫面，未變更鎖定設定。
- 測試版 APK 留在 Pad 6；兩個測試捷徑已移除，使用者可從 APP 長按重建。
- 紅米 Note 13 Pro+、其他 MIUI／HyperOS 版本及既有空白捷徑自動更新仍未驗證。
- 僅做上述 UI 回歸；未實際執行第三方 APP 下載、安裝或移除流程，未進行效能量測。
- 本機原始截圖、hierarchy、APK 備份與 build logs 位於 `app/build/pad6-validation-20261105/`，不納入 Git。
