# Fix Instant Run issues.
-keep class * extends android.content.ContentProvider
-keep class * extends android.app.Activity
-keep class * extends android.app.Service

# Avoid crash on some emulators when running the debug variant.
-keepclassmembers class com.facebook.android.crypto.keychain.SecureRandomFix$LinuxPRNGSecureRandom {
    public <init>(...);
}
