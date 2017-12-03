-optimizationpasses 10

-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,SourceFile,LineNumberTable

# Keep fields in R which are accessed through reflection.
-keepclasseswithmembers class **.R$* {
    public static final int define_*;
}

# Preserve annotated Javascript interface methods.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Suppress warnings about duplicate classes.
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**

# Remove all kinds of logging.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Remove Kotlin null checks.
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Avoid crash of SearchView.
-keep class android.support.v7.widget.SearchView { *; }

# Avoid crash on some emulators when running the debug variant.
-keepclassmembers class com.facebook.android.crypto.keychain.SecureRandomFix$LinuxPRNGSecureRandom {
    public <init>(...);
}

-keepclassmembers class com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder {
    public <init>(...);
}

-keepclassmembers class com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder {
    public <init>(...);
}

# Android Architecture Components
-keep class * implements android.arch.lifecycle.GeneratedAdapter {
    <init>(...);
}

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

-dontwarn com.bumptech.glide.R
-dontwarn com.bumptech.glide.integration.okhttp.R

# Iconics
-keepclassmembernames enum * implements com.mikepenz.iconics.typeface.IIcon { *; }

# Moshi
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# We use a custom parser for Hawk and exclude the Gson dependency.
-dontwarn com.orhanobut.hawk.HawkBuilder
-dontwarn com.orhanobut.hawk.HawkConverter**
-dontwarn com.orhanobut.hawk.GsonParser

# We exclude unused modules of ExoPlayer.
-dontwarn com.devbrackets.android.exomedia.core.source.builder.HlsMediaSourceBuilder
-dontwarn com.devbrackets.android.exomedia.core.source.builder.DashMediaSourceBuilder

# OkHttp/Okio/Retrofit
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ProxerLib
-keep enum me.proxer.library.** {
    **[] $VALUES;
    public *;
}
