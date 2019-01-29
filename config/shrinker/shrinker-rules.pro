# Keep annotations for refletive access.
-keepattributes *Annotation*

# Keep stacktraces readeable.
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

# Material Preference
-keepclassmembers class androidx.preference.PreferenceManager {
    void setNoCommit(boolean);
}

# Chromecast
-keepclasseswithmembers class androidx.mediarouter.app.MediaRouteActionProvider {
    public <init>(...);
}

# Iconics
-keepclassmembernames enum * implements com.mikepenz.iconics.typeface.IIcon { *; }
