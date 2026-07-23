# 寶可夢付費軟體購買指南

> [!WARNING]
> 本頁介紹的輔助軟體會修改遊戲、攔截遊戲功能（Hook）或注入程式碼，並非遊戲官方功能。使用時可能被偵測，導致警告、暫時停權或永久封鎖，也可能造成遊戲閃退、資料異常或裝置不穩定。付費版、進階版或官方宣稱的防偵測功能都不代表安全，也不保證帳號不會受罰；是否使用及相關損失須由使用者自行承擔。

> 最後查核：2026 年 7 月 24 日
> 軟體方案、點數與售價可能隨時調整，付款前請再次確認官方頁面顯示的方案、幣別、稅金與有效天數。
>
> **新台幣概算基準：**依 2026 年 7 月 23 日臺灣銀行即期賣出牌價，`US$1 約 NT$32.3`、`€1 約 NT$37.06`。下列新台幣金額只供快速估算，不包含國外交易手續費、匯差、VAT（加值型營業稅）或其他稅金。

## 購買前說明

- 本賣場以免費軟體為主，目前沒有提供代購服務。
- 若需要贊助（進階）版本，請自行透過軟體官方管道購買。
- 請勿向來路不明的賣家購買帳號、金鑰或破解版本。
- 先確認免費版本能在自己的裝置正常執行，再購買付費方案。
- 本頁只提供購買指引，不會代為聯絡官方、傳送 Discord 訊息或處理付款。
- 為了方便對照軟體介面，本頁會保留官方英文功能名稱；括號內的中文以台灣玩家容易理解的說法解釋功能，不採逐字直譯。
- 本頁沿用台灣玩家常說的「色違」與「星塵」。

## 快速比較

| 軟體 | 免費方案 | 主要付費方案 | 官方購買入口 |
|---|---|---|---|
| PGTools | PaC（自動捕捉）提供基本普通捕捉功能 | 依功能扣除 Coins（授權點數），基準價為 1 Coin＝US$1；可購買 3、7 或 30 天授權 | [PGTools 官方 Discord](https://discord.gg/pgtools) |
| Pokemod | Lite（免費版） | Pro（贊助版）：US$7／月，約 NT$226，最多可同時開啟 3 個 Pokemod | [Pokemod 官方網站](https://pokemod.dev/zh) |
| Shungo | 官方目前只列單一付費方案 | €14.99／月，約 NT$556，每份訂閱限 1 台裝置 | [Shungo 官方網站](https://shungo.app/) |
| PolygonX | Trial（試用版）提供有限的基本捕捉、轉牌及整理功能 | 透過 Patreon（會員贊助平台）取得 Credits（兌換點數），再兌換需要的金鑰 | [Evermore Labs 官方 Patreon](https://www.patreon.com/evermorelabs) |

## 依玩法選擇軟體

以下「建議」只代表功能是否符合玩法，不代表安全性較高。所有自動化、攔截或注入功能都有帳號處分風險；長時間無人操作、高速跨區或明顯超出正常遊戲行為，風險通常更高。

### 免費版完成度

- ✅ **可完成：** 免費版本身可完成主要流程，但速度或可調整項目可能少於付費版。
- 🟡 **部分完成：** 只能輔助手動操作、具有固定限制，或無法完成整個自動化流程。
- ❌ **無法完成：** 需要付費方案，或官方沒有提供可確認的免費版本。

| 免費方案 | 自動團體戰 | 主動偵測色違 | 自動抓怪刷星塵 | 道具與寶可夢整理 |
|---|---|---|---|---|
| PGTools PaC | ❌ PaR 才是自動團體戰方案 | ❌ PaS 才能依色違等條件主動狙擊 | ✅ 可用 Normal Mode（普通模式）自動捕捉及轉牌，但速度較慢 | ✅ 可設定傳送條件及道具保留數量 |
| Pokemod Lite | ❌ 沒有 Raid Pack（團體戰輔助） | 🟡 點進寶可夢的捕捉畫面後可略過非色違寶可夢，但不會主動掃描地圖或發出通知 | 🟡 可縮短手動點怪、投球及捕捉所需時間，但沒有 vPGP 虛擬 GO Plus 自動抓怪／轉補給站 | ✅ 可批次傳送，並提供部分道具及寶可夢整理功能 |
| Shungo | ❌ 官方沒有列出免費方案 | ❌ 官方沒有列出免費方案 | ❌ 官方沒有列出免費方案 | ❌ 官方沒有列出免費方案 |
| PolygonX Trial | ❌ Trial 不包含 Battler（自動戰鬥） | ❌ Trial 沒有主動色違掃描／狙擊 | 🟡 可基本自動捕捉及轉牌，但冷卻間隔、每日上限及篩選能力固定 | ✅ 可管理道具，並依條件傳送或批次傳送寶可夢 |

> 這裡的「主動偵測色違」是指主動掃描地圖或即時目標清單，找到色違後發出通知或自動前往目標。Pokemod Lite 的 `Block Non-Shiny Encounters`（略過非色違寶可夢）必須等玩家點進捕捉畫面後才能判斷，因此只算部分完成。

### 建議玩法與方案

#### 自動打團體戰

- **較完整的自動流程：** Shungo Raid（團體戰模式）可依城市、座標、目標及條件自動尋找團體戰並捕捉；PolygonX Battler（自動戰鬥）專門處理一般團體戰，以及 Dynamax／Gigantamax（極巨化／超極巨化）戰鬥。
- **PGTools：** PaR 可自動團體戰，但官方目前仍標示問題較多、穩定性有限，建議先買短天數測試。
- **Pokemod：** Pro 的 Raid Pack 會在已進入的團體戰內自動攻擊、閃避及略過部分動畫，但官方沒有表示它能自動搜尋團體戰、選擇頭目、進入大廳及完成捕捉，因此不能視為全自動團體戰功能。
- **免費版：** 目前沒有任何一套可確認能免費完成整段自動團體戰。

#### 自動打道館

「道館戰」與「團體戰」是不同功能。PolygonX 官方明確表示不會加入道館戰與 PvP（玩家對戰）自動戰鬥；其他本頁工具也沒有公開且可確認的完整自動道館攻防說明，因此本指南不把任何方案列為可自動打道館。

#### 自動偵測或捕捉色違

- **Pokemod Pro：** Shiny Scanner（色違偵測）會在地圖顯示附近色違並通知，可提供傳送或複製座標。
- **PGTools PaS：** 適合長時間依寶可夢、色違及其他條件自動狙擊。
- **Shungo Sniper：** 可依色違、IV（個體值）、PvP 聯盟、性別及尺寸等條件狙擊。
- **PolygonX：** Sniper（狙擊）或付費 Farmer（自動刷怪）的進階篩選可設定色違條件，實際功能及 Credits 消耗以 Discord 最新對照為準。
- **免費版：** Pokemod Lite 只能在點進捕捉畫面後略過非色違寶可夢；PGTools PaC 及 PolygonX Trial 即使可能在一般捕捉時碰巧抓到色違，也不能視為主動色違偵測。

#### 自動抓怪刷星塵、經驗值及糖果

- **免費優先：** PGTools PaC 免費版可用普通捕捉模式自動抓怪及轉牌，能持續取得捕捉帶來的星塵、經驗值及糖果。
- **免費但有限制：** PolygonX Trial 也能基本自動捕捉及轉牌，但使用固定冷卻間隔、每日上限及較少篩選功能。
- **手動輔助：** Pokemod Lite 可擴大寶可夢顯示範圍、加快進入捕捉畫面及投球的流程，但沒有虛擬 GO Plus，不能自行完成長時間自動抓怪。
- **付費效率方案：** 可考慮 Shungo Blatant（高速刷怪模式）、PolygonX Farmer、Pokemod Pro 的 vPGP 虛擬 GO Plus，或 PGTools PaC 的 Fast／Ultimate 贊助模式。
- 若要搭配 Star Piece（星星碎片）提高星塵收益，仍要確認所選方案是否能自動使用道具；不要只看到「自動抓怪」就假設會自動開星星碎片。

#### 自動孵蛋、夥伴與日常整理

- **自動孵蛋：** PGTools PaH、Pokemod Pro Eggspresso、PolygonX Farmer Lite 以上及 Shungo 付費方案都有相關功能；目前沒有可確認能完整自動放置孵化器及處理孵蛋流程的免費方案。
- **夥伴與禮物：** Pokemod Lite 可快速手動送禮／開禮，Pro 才有更多自動夥伴及批次功能；Shungo、PolygonX Farmer 與 PGTools 的相關自動化則依付費方案開放。
- **免費整理道具與寶可夢：** PGTools PaC、Pokemod Lite 與 PolygonX Trial 都能完成部分或大部分整理工作，適合先用免費版確認傳送條件及保留規則是否符合需求。

#### 自動火箭隊

- Shungo 的 Grunts（GO 火箭隊手下）／Blatant（高速刷怪）模式與 PolygonX 的付費自動化方案可處理 GO 火箭隊相關流程。
- PGTools 的 PaG／PaG U 已下架；Pokemod 的 Team Blastoff（火箭隊輔助）目前也標示為暫時無法運作。
- 免費版本目前沒有可確認能穩定完成整段自動火箭隊流程的方案。

---

## PGTools

### 官方入口

- 官方網站：[https://pgtools.net](https://pgtools.net)
- 官方 Discord：[https://discord.gg/pgtools](https://discord.gg/pgtools)
- 官方購買說明頻道：加入 Discord 後查看 `・support-us`
- 中文協助：前往 `chinese`（中文）頻道找 `GilbertMike`，或聯絡官方列出的 Global Admin／Supporter（管理員／客服人員）

> **注意：** 請勿在 PGTools 官方 Discord 內詢問 PolygonX 相關問題，PGTools 與 PolygonX 為不同作者開發的軟體。

### 方案、費用與主要功能

PGTools 使用 Coins（授權點數）購買指定功能與使用天數。依 PGTools Discord 於 2025 年 12 月 30 日的公開回覆，Coin 採固定美元基準價：`1 Coin＝US$1`；使用其他幣別付款時，再依當時匯率換算。

下列新台幣金額依本頁查核時的 `US$1 約 NT$32.3` 粗估。美元基準價固定不代表新台幣實付金額固定；匯率、Skrill（電子支付）、Ko-fi（贊助平台）、信用卡或其他付款管道的手續費均可能使實付金額增加，付款前仍應向官方授權人確認最終報價。

| 方案 | 官方費用 | 約新台幣 | 主要功能 |
|---|---:|---|---|
| PaC（自動捕捉） | 3 Coins／30 天（US$3） | 約 NT$97 | 自動捕捉、轉補給站、整理道具與寶可夢等功能 |
| PaS 試用（狙擊試用版） | 5 Coins／7 天（US$5） | 約 NT$162 | 自動狙擊、搜尋色違，以及依條件篩選與捕捉寶可夢 |
| PaS（狙擊完整版） | 12 Coins／30 天（US$12） | 約 NT$388 | PaS 完整版 |
| PaC + PaS（捕捉＋狙擊） | 13 Coins／30 天（US$13） | 約 NT$420 | 同時使用 PaC 與 PaS |
| PaH（自動孵蛋） | 1 Coin／30 天（US$1） | 約 NT$32 | Hatching Machine（自動孵蛋），可自動處理孵蛋相關流程 |
| PaK（變隱龍搜尋） | 3 Coins／30 天（US$3） | 約 NT$97 | Kecleon Hunter（變隱龍搜尋），可搜尋變隱龍 |
| PaR（自動團體戰） | 3 Coins／3 天（US$3） | 約 NT$97 | 自動團體戰；目前仍有較多問題，穩定性有限 |
| PaR（自動團體戰） | 5 Coins／7 天（US$5） | 約 NT$162 | 同上，可切換遊戲帳號，官方中文說明標示共 3 次 |
| PaR（自動團體戰） | 12 Coins／30 天（US$12） | 約 NT$388 | 同上，可切換遊戲帳號，官方中文說明標示共 3 次 |

目前官方中文說明已明確標示：`PaG`／`PaG U` 自動火箭隊方案已下架，不要再依照舊教學購買。

### 購買步驟

1. 從 [PGTools 官方網站](https://pgtools.net) 下載 `FINAL APK`（正式版安裝檔）。
2. 使用免費功能測試 PGTools、Pokémon GO 與 GPS Joystick 是否能在裝置上正常運作。
3. 加入 [PGTools 官方 Discord](https://discord.gg/pgtools)。
4. 先查看 `・support-us` 頻道的最新方案與授權人名單。
5. 如需中文協助，到 `chinese` 頻道找到 `GilbertMike`。
6. 告知要購買的方案、天數與所需 Coins，並向對方確認實際付款金額。
7. 官方支援名單目前將 `GilbertMike` 列為中國、香港等地區的協助人員，可使用 Alipay（支付寶）、WeChat（微信）、PayMe、FPS（轉數快）、Google Pay、Apple Pay、Wise（跨境匯款）、Ko-fi（贊助平台）或 Crypto（加密貨幣）；Skrill（電子支付）、PayPal 等方式則由其他客服人員提供。實際可用方式仍以購買當下回覆為準。
8. 成為贊助者後會取得 PGTools 帳號。日後加值時，只需要提供 PGTools 使用者名稱或 ID，不要提供密碼。
9. 可使用官方的 [Discord 贊助身分連結](https://management.pgtools.net/oauth2/discord) 將 PGTools 授權連結到 Discord，取得 Sponsor（贊助者）身分組。

### 使用限制

- 僅支援實體且已取得 Root（最高系統權限）的 Android 裝置。
- 不支援模擬器、MuMu、BlueStacks、VMOS 或 iOS。
- 官方要求先測試免費 PaC，確認可以運作後再購買授權。
- 2026 年 7 月的官方中文說明指出，Android 14 以上仍可能出現嚴重閃退；PaR 目前也仍不穩定。

---

## Pokemod

### 官方入口

- 官方網站：[https://pokemod.dev/zh](https://pokemod.dev/zh)
- 官方 Discord：[https://discord.gg/pokemod](https://discord.gg/pokemod)
- 官方 Patreon：[https://www.patreon.com/pokemod/join](https://www.patreon.com/pokemod/join)
- 其他付款方式：[https://pokemod.dev/zh/payment](https://pokemod.dev/zh/payment)

### 方案與費用

| 方案 | 費用 | 使用限制 | 說明 |
|---|---:|---|---|
| Lite（免費版） | 免費 | Account Manager（帳號管理）最多儲存 2 個 Pokémon GO 帳號 | 提供捕捉、IV（個體值）、道具、送禮及定位修正等基本功能 |
| Pro（贊助版） | US$7／月（約 NT$226） | 最多可同時開啟 3 個 Pokemod；Account Manager 不限制儲存帳號數 | 包含 Lite 功能、Pro 專屬自動化功能、最新版本與 Discord Pro 身分組 |

Patreon 結帳時可能依所在地加上稅金或顯示換算後的當地幣別，以結帳頁面為準。

### Lite（免費版）主要功能

- Account Manager（帳號管理）：快速儲存及切換 Pokémon GO 帳號；Lite 最多儲存 2 個帳號。
- Encounter Stats（捕捉畫面資訊）：在捕捉畫面顯示 IV（個體值）、體型、展示會分數及特殊背景等資訊。
- Encounter Tweaks（捕捉畫面調整）：縮短進入捕捉畫面的時間；可略過非色違寶可夢，或在捕捉成功後直接回到地圖。
- First Aid Kit（快速操作輔助）：略過部分對話及動畫、快速選取道具數量，並顯示額外的捕捉或交換資訊；部分進階項目僅限 Pro。
- Integrated Mock Location Patch（內建模擬定位修正）：不需要另外使用 LSPosed 或 Smali Patcher 即可處理錯誤 12。
- Instant Spin（快速轉牌）：點擊補給站即可直接轉牌；誤觸仍可能意外觸發冷卻時間。
- Inventory Tweaks（寶可夢列表調整）：Lite 可批次傳送寶可夢；批次進化、超級進化、淨化及其他進階批次操作僅限 Pro。
- Perfect Throw（投球輔助）：可隨機投出 Excellent 曲球、取回完全投失的球，並提供其他投球輔助；Headshot（瞄準中心點）僅限 Pro。
- Show IVs in Inventory（在寶可夢列表顯示個體值）：以 IV 及展示會分數取代寶可夢名稱。
- Spawn Booster（擴大寶可夢顯示範圍）：讓地圖顯示更遠處的寶可夢。
- Swift Gift（快速送禮）：快速送禮、開禮、略過動畫並處理明信片。

### Pro（贊助版）額外功能

Pro 包含全部 Lite 功能，並額外提供：

- Berry Master（自動餵樹果）：依設定在捕捉畫面自動餵食指定樹果。
- Cooldown Tracker（冷卻時間紀錄）：記錄會觸發冷卻的操作、顯示剩餘時間，並降低冷卻期間誤投精靈球的風險。
- Eggspresso（自動孵蛋）：自動放置孵化器及處理孵蛋流程。
- Enhanced Radar Positioning（雷達定位加強）與 Live Feed（即時目標清單）：提供更多地圖目標資訊、篩選及快速定位功能。
- Incognito Mode（隱私模式）、Instant Loading（快速載入）與 Performance Mode（效能模式）。
- Magic Bag（自動整理道具）與 Mass Gift（批次送禮）。
- PvP Metrics（玩家對戰資訊）與 Quest Tweaks（調查課題輔助）。
- Raid Pack（團體戰輔助）、Shiny Scanner（色違偵測）與 Tamagotchi（自動照顧夥伴）。
- Tap to Teleport（點擊傳送）：需搭配 App Ninjas GPS Joystick（GPS 定位搖桿）。
- Team Blastoff（火箭隊輔助）：官方功能表目前標示為暫時無法運作。
- Transfer on Catch（捕捉後自動傳送）。
- vPGP³（虛擬 GO Plus）：自動捕捉、自動轉牌及自動重新連線。

### 購買步驟

1. 先從 [Pokemod 官方網站](https://pokemod.dev/zh) 安裝並測試 Lite。
2. 確認已取得 Root（最高系統權限）的 Android 裝置可以正常啟動 Pokemod，並使用 Play 商店版 Pokémon GO。
3. 點選官網的「訂閱／加入我們」，或直接前往 [官方 Patreon](https://www.patreon.com/pokemod/join)。
4. 如 Patreon 不適用，可使用官網的[其他付款方式頁面](https://pokemod.dev/zh/payment)，依國家選擇可用管道。
5. 完成訂閱後，依官方流程連結 Discord，取得 Pro 身分組並使用最新 Pokemod Pro。

> 官網另列有 Bitcoin（比特幣）「贊助我們」地址，但該區塊不等同於自動開通 Pro。若要取得 Pro，請使用明確標示的訂閱或替代付款流程。

---

## Shungo

### 官方入口

- 官方網站：[https://shungo.app/](https://shungo.app/)
- 官方使用說明：[https://docs.shungo.app/](https://docs.shungo.app/)
- 官方 Discord：[https://discord.gg/shungo](https://discord.gg/shungo)

### 方案與費用

| 方案 | 費用 | 裝置數 | 說明 |
|---|---:|---:|---|
| Shungo 月費方案 | €14.99／月（約 NT$556） | 1 台 | 包含所有功能與後續更新，每月自動續訂，可從網站帳號後台取消 |

官方目前只公開一種付費方案，沒有列出可確認的免費試用。若要同時在多台裝置使用，每台裝置都需要個別訂閱；帳號共用可能導致授權被撤銷且不退款，訂閱也不能轉移到另一個帳號。

### 主要模式與功能

- Sniper（狙擊模式）：依寶可夢、色違、IV（個體值）、PvP（玩家對戰）聯盟、性別及尺寸等條件搜尋並捕捉目標。
- Raid（團體戰模式）：依城市、自訂座標與範圍自動進行一般團體戰並捕捉目標。
- Dynamax（極巨化模式）：自動處理 Dynamax／Gigantamax（極巨化／超極巨化）戰鬥。
- Blatant（高速刷怪模式）：高速捕捉、轉補給站、擊敗 GO 火箭隊成員，並累積經驗值、星塵及糖果等資源。
- Grunts（GO 火箭隊手下模式）：自動處理 GO 火箭隊手下相關流程。
- Manager（日常管理模式）：處理禮物、夥伴、治療、道具及寶可夢整理。
- Kecleon（變隱龍搜尋模式）：搜尋變隱龍。

### 裝置需求與限制

- 僅支援已取得 Root（最高系統權限）的 Android 裝置，不支援 iOS。
- 一份訂閱預設只能綁定 1 台裝置。
- 官方說明宣稱支援各版本 Pokémon GO，但遊戲更新後仍應先確認 Shungo 最新公告及相容狀態。
- Shungo 內建定位功能；Blatant（高速刷怪）模式若要依路線移動，官方仍建議搭配 Joystick（定位搖桿）。

### 購買步驟

1. 前往 [Shungo 官方網站](https://shungo.app/)註冊或登入帳號。
2. 先確認登入方式及帳號正確；相同電子郵件若分別透過 Google、Discord、Facebook 或 Shungo 登入，可能被建立成不同帳號。
3. 點選 `Buy Now`（立即購買）或 `Get Started`（開始使用），確認方案為 `€14.99／月（約 NT$556）`、訂閱帳號及自動續訂條件。
4. 透過官方導向的 Stripe（線上金流平台）結帳頁完成付款；官方列出的方式包含信用卡、Google Pay 與 Apple Pay，也可透過 Google Pay／Apple Pay 使用 PayPal。
5. 回到網站帳號後台確認訂閱狀態，再依[官方安裝說明](https://docs.shungo.app/)完成設定。
6. 若不再使用，請在下次續訂日前從網站帳號後台取消。

---

## PolygonX

### 官方入口

- 開發團隊：Evermore Labs
- 官方 Patreon：[https://www.patreon.com/evermorelabs](https://www.patreon.com/evermorelabs)
- 目前可用的一般邀請：[https://discord.gg/Uebm3ebsjd](https://discord.gg/Uebm3ebsjd)

上述分享連結目前有效，會進入 `PolygonX` 伺服器；這是一般分享連結，並不是 PolygonX 官方自訂短網址。

### Patreon 會員費用

PolygonX 透過 Evermore Labs 的 Patreon（會員贊助平台）提供 Credits（兌換點數），再使用點數取得 PolygonX 金鑰。以下為 2026 年 7 月 24 日官方頁面顯示的 EUR（歐元）月費，結帳時另加 VAT（加值型營業稅）：

| Patreon 等級 | 每月費用（約新台幣） | PolygonX 兌換點數（Credits） |
|---|---:|---:|
| Wood Tier（木材級） | €4.99（約 NT$185） | 5 |
| Stone Tier（石材級） | €9.99（約 NT$370） | 10 |
| Redstone Tier（紅石級） | €14.99（約 NT$556） | 15 |
| Iron Tier（鐵級） | €18.99（約 NT$704） | 20 |
| Gold Tier（黃金級） | €26.99（約 NT$1,000） | 30 |
| Lapislazuli Tier（青金石級） | €35.99（約 NT$1,334） | 40 |
| Emerald Tier（綠寶石級） | €49.99（約 NT$1,853） | 60 |
| Quartz Tier（石英級） | €69.99（約 NT$2,594） | 90 |
| Diamond Tier（鑽石級） | €99.99（約 NT$3,706） | 150 |
| Netherite Tier（獄髓級） | €199.99（約 NT$7,412） | 300 |

Patreon 可能依訪客所在地將上述歐元基準價換算成美元、新加坡幣或其他支援幣別顯示；這是 Patreon 的地區幣別換算，不代表 PolygonX 方案或 Credits 數量已改價。實付仍以結帳頁顯示的幣別、換算金額及 VAT 為準。

### 免費 Trial（試用版）

PolygonX 官方 Discord 的公開功能表目前列有 Trial（試用版），不需要先購買 Patreon Credits。官方列出的限制及基本功能包含：

- 1.4 公里 Geofence（活動範圍限制）。
- 道具管理，以及依條件傳送或批次傳送寶可夢。
- 道具不足時進行基本轉牌，並可設定捕捉所需的最低寶可夢球數。
- 不含篩選條件的基本捕捉。
- 固定且不可自訂的冷卻間隔與每日上限。

Trial 適合先確認裝置、官方 Pokémon GO 與 PolygonX 是否能正常運作；付費前仍應先以 Trial 實測。

### 金鑰種類與所需點數

Evermore Labs 的公開 Patreon 只列出「會員費用與可取得的 Credits」，沒有公開列出每種金鑰的 Credits 消耗。下表來自目前可查到的社群教學，付款前必須再以 PolygonX Discord 內的最新金鑰表為準。

| 金鑰 | 社群資料所列點數 | 對應用途 | 可涵蓋點數的最低 Patreon 等級 |
|---|---:|---|---|
| Farmer Lite（自動刷怪輕量版） | 5 | 基本自動刷怪及重複操作 | Wood Tier（木材級） |
| Farmer Standard（自動刷怪標準版） | 10 | 自動刷怪的標準方案 | Stone Tier（石材級） |
| Farmer Premium（自動刷怪進階版） | 30 | 提供更多自動化及篩選功能 | Gold Tier（黃金級） |
| Farmer Platinum（自動刷怪白金版） | 60 | 高階自動刷怪方案 | Emerald Tier（綠寶石級） |
| Farmer Titanium（自動刷怪鈦金版） | 140 | 最高階自動刷怪方案 | Diamond Tier（鑽石級，150 Credits） |
| Sniper（狙擊） | 10 | 狙擊及目標篩選 | Stone Tier（石材級） |
| Battler（自動戰鬥） | 15 | 團體戰及 Dynamax／Gigantamax（極巨化／超極巨化）戰鬥自動化 | Redstone Tier（紅石級） |

> PolygonX 官方 Discord 於 2026 年 2 月 28 日更新的公開功能表列有 Farmer Lite／Standard／Premium／Platinum／Titanium、Sniper 與 Battler，但已找不到舊社群資料所列的 Purifier（自動淨化）獨立方案，因此本指南不再把 Purifier 列為可購買的現行金鑰。各金鑰的 Credits 消耗仍未列在公開官方頁面，請在付款前查看 Discord 的最新價格與功能對照，不要只依照上表直接購買。

### 官方公開列出的主要功能

- 自動捕捉與傳送寶可夢。
- 自動旋轉補給站。
- 自動進行火箭隊戰鬥。
- 自動進行傳說團體戰，以及 Dynamax（極巨化）與 Gigantamax（超極巨化）戰鬥。
- 可依方案使用目標篩選、行動優先順序、活動範圍限制及其他 Farmer（自動刷怪）功能。
- 官方明確表示不會加入道館戰與 PvP（玩家對戰）自動戰鬥等會直接影響其他玩家的功能。

### 購買步驟

1. 先使用 Trial（試用版）確認裝置環境可以正常啟動 PolygonX 與官方 Pokémon GO。
2. 前往 [Evermore Labs 官方 Patreon](https://www.patreon.com/evermorelabs)。
3. 依需要的金鑰點數選擇足夠的 Patreon 等級，確認 EUR（歐元）基準價、Patreon 顯示的結帳幣別、VAT（加值型營業稅）與每月續訂條件。
4. 完成 Patreon 會員後，依官方提示連結 Discord。
5. 在 PolygonX Discord 查看最新的金鑰點數表、下載與啟用說明。
6. 確認收到的 Credits（兌換點數）足夠後，再依官方流程取得需要的 PolygonX 金鑰。

### PolygonX Discord 短網址說明

- `https://discord.gg/polygonx` 目前實測為無效邀請。
- Evermore Labs 官方 Patreon 文字目前仍寫著 `https://discord.com/polygonx`，但該網址實測會進入 Discord 的找不到網頁畫面。
- 目前沒有查到可正常使用、且由官方公開的 `polygonx` 自訂後綴。
- 這不代表 PolygonX 一定不是「社群伺服器」。Discord 的自訂邀請後綴主要要求伺服器維持最高 Boost（伺服器加成）等級 Level 3（第 3 級），並不是只要啟用社群功能就會自動取得。
- 一般邀請碼可能被建立者撤銷或設定期限；如果目前連結失效，需要由仍在伺服器內的成員重新產生。

---

## 資料來源

- [PGTools 官方網站](https://pgtools.net)
- [PGTools 官方管理介面](https://management.pgtools.net/app-screen)
- [PGTools 官方 Discord](https://discord.gg/pgtools) 的 `・support-us`、`features📝`、`chinese` 頻道，以及 2025 年 12 月 30 日關於 Coin 美元基準價的公開回覆
- [Pokemod 官方網站](https://pokemod.dev/zh)
- [Pokemod 官方 Patreon](https://www.patreon.com/pokemod/join)
- [Pokemod 官方 Discord](https://discord.gg/pokemod) 的 `features` 與 `faq-pokemod` 頻道
- [Shungo 官方網站](https://shungo.app/)
- [Shungo 官方 FAQ](https://docs.shungo.app/info/faq)
- [Shungo 官方功能說明](https://docs.shungo.app/info/showcase)
- [Shungo 官方 Raid（團體戰）模式說明](https://docs.shungo.app/guides/raid-mode)
- [Shungo 官方 Sniper（狙擊）模式說明](https://docs.shungo.app/guides/sniper-mode)
- [Shungo 官方購買說明](https://docs.shungo.app/info/purchase)
- [Shungo 官方 Discord](https://discord.gg/shungo)
- [Evermore Labs 官方 Patreon](https://www.patreon.com/evermorelabs)
- PolygonX 官方 Discord 的 `features` 頻道
- [Discord 官方自訂邀請連結說明](https://support.discord.com/hc/en-us/articles/115001542132-Custom-Invite-Link)
- [PolygonX 中文社群教學（非官方，僅用於金鑰點數交叉參考）](https://sites.google.com/view/lineid-vawa31/)
- [Pokémon GO 官方遊戲用語簡介](https://niantic.helpshift.com/hc/zh-hant/6-pokemon-go/faq/122-glossary/?hl=zh_hant&l=zh-Hant)
- [Pokémon GO 官方捕捉說明](https://niantic.helpshift.com/hc/zh-hant/6-pokemon-go/faq/102-finding-catching-wild-pokemon/?l=zh-Hant&p=web)
- [臺灣銀行牌告匯率（新台幣概算依據）](https://rate.bot.com.tw/xrt?Lang=zh-TW)
