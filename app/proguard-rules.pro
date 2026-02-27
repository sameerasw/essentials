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
#-renamesourcefileattribute SourceFile

# Gson rules
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Shizuku rules
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }

# Kotlin Reflect
-keep class kotlin.reflect.** { *; }
-keep class com.sameerasw.essentials.domain.model.** { *; }

# Prevent over-minification of settings and registry classes
-keep class com.sameerasw.essentials.data.repository.** { *; }
-keep class com.sameerasw.essentials.domain.registry.** { *; }