# 開發參考與外部工具

[返回首頁](../README.md) · [第三方聲明](../THIRD_PARTY_NOTICES.md)

這份清單保留原 README 的致謝，但區分「實際包含的內容」與「外部工具／參考」。列出專案不代表官方合作、背書或 runtime dependency。

| 專案 | 目前用途與分類 |
| --- | --- |
| [DetectMagiskHide](https://github.com/darvincisec/DetectMagiskHide) | 已確認部分原始碼直接取用／移植；不是僅參考。範圍與 Apache-2.0 attribution 見 [第三方聲明](../THIRD_PARTY_NOTICES.md#detectmagiskhide)。 |
| [Simple Play Integrity Checker](https://github.com/herzhenr/spic-android) | 外部完整性檢測 APP，透過套件啟動入口開啟；本次未找到將其檢測程式碼內嵌的證據。 |
| [ApplistDetector](https://github.com/Dr-TSNG/ApplistDetector) | 歷史設備檢測參考；本次未找到直接複製程式碼的證據。 |
| [Ruru](https://github.com/byxiaorun/Ruru) | 外部環境檢測 APP／下載連結，並非本 APP 內嵌的檢測 library。 |
| [LSPosed Manager](https://github.com/LSPosed/LSPosed) | 歷史 UI 參考；共同 Android／Material 結構不足以認定程式碼衍生。 |
| [Apktool](https://github.com/iBotPeaches/Apktool) | APK 分析開發工具，沒有作為 APP runtime dependency 打包。 |
| [jadx](https://github.com/skylot/jadx) | APK 分析開發工具，沒有作為 APP runtime dependency 打包。 |
| [Discord-User-API](https://github.com/netgian/Discord-User-API) | 歷史參考，疑似移植實作已於 2024-10-06 移除；不能據此宣告舊 APK 或歷史程式碼的授權已清算。 |

沒有找到直接複製的證據，不等於已證明所有本地程式碼均為原創。已確認的 attribution 與待確認授權事項記錄在[第三方聲明](../THIRD_PARTY_NOTICES.md)。

## English summary

DetectMagiskHide contributes confirmed derived source portions; its notices are recorded separately. SPIC and Ruru are external apps, ApplistDetector and LSPosed are development references, and Apktool/jadx are external development tools. The historical Discord-related implementation was removed from current source, but its provenance and older binaries still require clarification. None of these references implies affiliation.
