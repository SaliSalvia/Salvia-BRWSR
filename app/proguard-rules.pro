# ---- JNI and JS bridge ----
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.browxer.engine.sniffer.MediaSnifferBridge { *; }

# ---- yt-dlp / youtubedl-android (JNI: bundles Python 3.8) ----
-keep class com.yausername.** { *; }
-keep class com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.**

# ---- FFmpeg ----
-keep class com.arthenica.** { *; }
-dontwarn com.arthenica.**

# ---- NewPipe ----
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.**

# ---- Kotlin serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }

# ---- Media3 ----
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---- Coroutines debug metadata ----
-dontwarn kotlinx.coroutines.debug.**

# ---- OkHttp / Conscrypt (ytdl headers) ----
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
