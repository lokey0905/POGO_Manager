# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile++

#壓縮比，預設5不修改
-optimizationpasses 5

#不使用大小寫混合，混淆後類名稱為小寫
-dontusemixedcaseclassnames

#指定不去忽略公開的 publicli classes
-dontskipnonpubliclibraryclasses

#混淆後產生印射文件
-verbose

#註解此行，可以自動上傳 mapping 檔到 Firebase
#-printmapping mapping.txt

#保留泛型
-keepattributes Signature

# 不做預校驗，加速建置速度
-dontpreverify

# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
-keepattributes Signature

# 抛出異常時保留檔名與行數
-keepattributes SourceFile,LineNumberTable

# 保留 android-support
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

-keepclassmembers class **.R$* {
    public static <fields>;
}