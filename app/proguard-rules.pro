-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep class android.support.** { *; }
-keep interface android.support.** { *; }

-dontwarn com.orhanobut.hawk.**
-keep class com.google.gson.** { *; }
-keepattributes Signature

-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

-dontwarn okio.**
-dontwarn com.birbit.android.jobqueue.scheduling.**