# Keep stacktraces readeable.
-keepattributes SourceFile,LineNumberTable

# Keep essential support library class.
-keep,allowoptimization class androidx.core.app.CoreComponentFactory { *; }

# Keep fields in R which are accessed through reflection.
-keepclasseswithmembers,allowoptimization class **.R$* {
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

# Material Preference
-keepclassmembers,allowoptimization class androidx.preference.PreferenceManager {
    void setNoCommit(boolean);
}

# Cast
-keepclasseswithmembers,allowoptimization class androidx.mediarouter.app.MediaRouteActionProvider {
    public <init>(...);
}

# Fix for Community-Material-Typeface
-keepclassmembers,allowoptimization enum com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial$Icon* { *; }

# Keep constructor of keychain class.
-keepclasseswithmembers class com.facebook.android.crypto.keychain.SecureRandomFix$LinuxPRNGSecureRandom {
    public <init>(...);
}

# TODO: Remove once it works without.
# Workaround crash in Google Cast.
-keep,allowoptimization class com.google.android.gms.internal.cast.** { *; }

# Ignore warnings about specific classes not being available on Android JDK.
-dontwarn java.util.concurrent.Flow$*
