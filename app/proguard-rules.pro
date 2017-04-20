-optimizations !field/*,!class/merging/*
-optimizationpasses 100
-allowaccessmodification
-dontpreverify

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

-keepattributes *Annotation*
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version. We know about them, and they are safe.
-dontwarn android.support.**

# EventBus
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Moshi
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# We use a custom parser for Hawk and exclude the Gson dependency
-dontwarn com.orhanobut.hawk.**

# OkHttp/Okio
-dontwarn okio.**

# Retrofit
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ProxerLib
-keep public enum me.proxer.library.enums.** {
    **[] $VALUES;
    public *;
}

# DBFlow
-keep class * extends com.raizlabs.android.dbflow.config.DatabaseHolder { *; }

# Avoid crash of SearchView
-keep class android.support.v7.widget.SearchView { *; }

# Avoid crash on some emulators when running the debug variant
-keepclassmembers class com.facebook.android.crypto.keychain.SecureRandomFix$LinuxPRNGSecureRandom {
   public <init>(...);
}

-keepclassmembers class * implements com.google.android.exoplayer.extractor.Extractor {
   public <init>(...);
}

-keepclassmembers class * implements com.google.android.exoplayer.text.SubtitleParser  {
   public <init>(...);
}

-keepclassmembers class com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder {
   public <init>(...);
}

-keepclassmembers class com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder {
   public <init>(...);
}
