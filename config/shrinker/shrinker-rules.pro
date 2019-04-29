# Keep stacktraces readeable.
-keepattributes SourceFile,LineNumberTable

# Keep essential support library class.
-keep class androidx.core.app.CoreComponentFactory { *; }

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

# Material Preference
-keepclassmembers class androidx.preference.PreferenceManager {
    void setNoCommit(boolean);
}

# Cast
-keepclasseswithmembers class androidx.mediarouter.app.MediaRouteActionProvider {
    public <init>(...);
}

# Recent versions of R8 seem to require this: https://issuetracker.google.com/issues/123558494
-keep @com.squareup.moshi.JsonQualifier @interface *

# Keep constructor of keychain class.
-keepclasseswithmembers class com.facebook.android.crypto.keychain.SecureRandomFix$LinuxPRNGSecureRandom {
    public <init>(...);
}
