# Keep line numbers so Crashlytics stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# --- Koin: modules resolve dependencies by type, so constructors must survive ---
-keep class com.gps.zazor.**ViewModel* { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# --- Custom views inflated by name from XML ---
-keep class com.gps.zazor.views.** { *; }

# --- Data and state classes read reflectively by Room / the bundle delegate ---
-keep class com.gps.zazor.data.models.** { *; }
-keep class com.gps.zazor.data.storage.models.** { *; }

# --- Dermandar panorama SDK (obfuscated third-party jar, uses JNI and reflection) ---
-keep class com.dermandar.** { *; }
-dontwarn com.dermandar.**

# --- Glide ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# --- Joda-Time ships classes it does not need on Android ---
-dontwarn org.joda.convert.**
-dontwarn org.joda.time.**
-keep class org.joda.time.** { *; }

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**
