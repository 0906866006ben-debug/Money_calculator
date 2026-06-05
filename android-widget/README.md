# MoneyCalculator Android Widget

真正的 Android 桌面小工具版本。

## 功能

- 桌面 widget 直接顯示止損距離、建議槓桿、每單保證金。
- `-1.0 / -0.1 / +0.1 / +1.0` 短按調整止損距離。
- 每單保證金固定為總資金 `2%`。
- 目標 1R 固定為總資金 `1%`。
- 點 widget 標題或總資金可重新設定總資金。
- 長按桌面 widget 通常會被 Launcher 用來移動或調整小工具，因此不使用長按連續加減。
- 不連網、不需要任何帳號或 API。

## 打包方式

這台電腦目前沒有偵測到 Java、Gradle 或 Android SDK，所以這裡先提供 Android Studio 可開啟的專案。

1. 安裝 Android Studio。
2. 用 Android Studio 開啟 `android-widget/` 這個資料夾。
3. 等 Gradle sync 完成。
4. 連接 Android 手機並開啟 USB 偵錯。
5. 按 Run 安裝到手機。

## 用 GitHub 下載 APK

根目錄已加入 GitHub Actions workflow：

```text
.github/workflows/build-android-apk.yml
```

把整個 `MoneyCalculator` 資料夾推到 GitHub 後，可以用兩種方式下載 APK。

### 方式 A：Actions artifact

1. 打開 GitHub repo。
2. 進入 `Actions`。
3. 選 `Build Android APK`。
4. 點最新一次成功的 run。
5. 在 `Artifacts` 下載 `MoneyCalculatorWidget-debug-apk`。
6. 解壓縮後安裝 `MoneyCalculatorWidget-debug.apk`。

### 方式 B：GitHub Releases

推一個 `v` 開頭的 tag，GitHub 會自動建立 Release 並附上 APK：

```powershell
git tag v1.0.0
git push origin v1.0.0
```

完成後到 GitHub repo 的 `Releases` 下載：

```text
MoneyCalculatorWidget-debug.apk
```

這是 debug-signed APK，適合自己手機安裝測試。Android 可能會提示「未知來源」或「不明應用程式」，需要允許瀏覽器或檔案管理器安裝 APK。

## 手機加入桌面小工具

1. 手機安裝 App 後，回到桌面。
2. 長按桌面空白處。
3. 選「小工具」或「Widgets」。
4. 找到「倉位計算」。
5. 拖到桌面。
6. 設定總資金後儲存。

## 計算驗收

- 總資金 `100U`、止損 `4.8%` -> 保證金 `2.00U`、建議槓桿 `10x`。
- 按 `+0.1` 到 `4.9%` -> 保證金 `2.00U`、建議槓桿 `10x`。
- 按 `+1.0` 到 `5.8%` -> 保證金 `2.00U`、建議槓桿 `8x`。
