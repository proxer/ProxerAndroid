# Keep line numbers to make stacktraces readable.
-keepattributes SourceFile,LineNumberTable

# Keep fields in R which are accessed through reflection.
-keepclasseswithmembers class **.R$* {
    public static final int define_*;
}

# Remove all kinds of logging.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Support preference
-keepclassmembers class androidx.preference.PreferenceManager {
    void setNoCommit(boolean);
}

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# Iconics
-keepclassmembernames enum * implements com.mikepenz.iconics.typeface.IIcon { *; }

# Hawk
-dontwarn com.orhanobut.hawk.HawkConverter**
-dontwarn com.orhanobut.hawk.HawkBuilder
-dontwarn com.orhanobut.hawk.GsonParser

# ExoMedia
-dontwarn com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer$DelegatedMediaDrmCallback

# RxBinding
-dontwarn com.jakewharton.rxbinding2.support.design.widget.TabLayoutSelectionEventObservable$Listener
-dontwarn com.jakewharton.rxbinding2.support.design.widget.TabLayoutSelectionEventObservable
-dontwarn com.jakewharton.rxbinding2.support.design.widget.TabLayoutSelectionsObservable$Listener
-dontwarn com.jakewharton.rxbinding2.support.design.widget.TabLayoutSelectionsObservable

# Moshi
-keep @com.squareup.moshi.JsonQualifier interface *
-keepnames @com.squareup.moshi.JsonClass class *

-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}

# Retrofit
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.internal.platform.ConscryptPlatform

-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Okio
-dontwarn okio.**

# Workaround missing rule in WorkManager library: https://issuetracker.google.com/issues/116296569
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ProxerLib
-keepclassmembers enum me.proxer.library.** {
    <fields>;
}
