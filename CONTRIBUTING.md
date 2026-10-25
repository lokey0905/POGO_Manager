# 貢獻指南 / Contributing

[繁體中文](#繁體中文) | [English](#english)

## 繁體中文

### 參與貢獻

歡迎回報錯誤、相容性問題、修正文檔，或提交範圍明確的 pull request。

1. 回報前先查閱既有 [issues](https://github.com/lokey0905/POGO_Manager/issues)。較大的整合或架構調整請先討論。
2. 保持修改範圍集中；除非就是本次修改目的，否則請保留既有下載、安裝偵測與版本選擇行為。
3. 解析器、版本比較及狀態邏輯變更應補上回歸測試。UI 調整請依需要說明手機／平板、旋轉及字體縮放的測試情況。
4. 依照[開發文件的建置與測試步驟](docs/development.md#建置與測試)執行驗證，並在 pull request 說明通過、失敗及尚未驗證的裝置行為。請勿提交 APK、憑證、簽章金鑰或個人裝置資料。

### 問題回報

請提供足以重現問題的資訊：

- POGO Manager 版本與安裝來源；自行建置時請附 commit hash。
- 裝置型號、Android 版本、廠商韌體，以及相關的處理器架構或 root／框架設定。
- 受影響的遊戲／工具版本、選擇的目標遊戲版本、正式／測試版通道與下載來源。
- 重現步驟、預期結果、實際結果，以及重新整理後是否改變。
- 相關截圖或錯誤發生前後的簡短 logcat。布局問題請附畫面方向、顯示尺寸及字體大小；上游查詢問題請附發生時間與已知的端點。

公開紀錄前請遮蔽帳號、token、裝置識別碼、精確位置等個人資訊。不要為了重現問題而上傳登入憑證或非公開 APK。

### 來源與授權

本專案目前尚未設定專案層級授權。第三方內容的來源與待確認事項見[第三方聲明](THIRD_PARTY_NOTICES.md#unresolved-included-material-and-distribution-limits)。新增第三方程式碼時，請提供來源、版本、授權與修改範圍，保留必要聲明；不要將未確認來源的內容當成原創。本文件不是貢獻者授權協議，也不自動重新授權既有內容。

## English

### Contributing

Bug reports, compatibility reports, documentation corrections, and focused pull requests are welcome.

1. Check existing [issues](https://github.com/lokey0905/POGO_Manager/issues) before opening a new report. Discuss larger integration or architecture changes first.
2. Keep changes scoped and preserve existing download, installation-detection, and version-selection behavior unless that is the intended change.
3. Add regression tests for parser, version-comparison, and state-logic changes. For UI work, describe phone/tablet, rotation, and font-scale checks as applicable.
4. Follow the development guide's [build and test steps](docs/development.en.md#build-and-test), and report what was tested, what failed, and any untested device-specific behavior in the pull request. Do not include APKs, credentials, signing keys, or personal device data in a contribution.

### Reporting issues

Include enough information to reproduce the problem:

- POGO Manager version and installation source; commit hash if built locally.
- Device model, Android version, OEM firmware, and relevant architecture or root/framework configuration.
- Affected game/tool versions, selected target game version, stable/test channel, and download source.
- Steps to reproduce, expected result, actual result, and whether refreshing changes it.
- Relevant screenshots or a short logcat excerpt around the failure. For layout problems, include orientation, display size, and font scale; for upstream failures, include the time and endpoint if known.

Redact account identifiers, tokens, device identifiers, precise location, and other personal information before posting logs. Do not post credentials or private APKs to demonstrate a problem.

### Provenance and licensing

No project-level license has been adopted. Sources and unresolved questions about third-party material are recorded in the [third-party notices](THIRD_PARTY_NOTICES.md#unresolved-included-material-and-distribution-limits). Identify the source, revision, license, and modifications for third-party code, and retain required notices. Do not describe uncertain sources as original work. This document is not a contributor license agreement and does not relicense existing material.
