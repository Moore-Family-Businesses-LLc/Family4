# ════════════════════════════════════════════════════════════════════════════
# Family4 — ProGuard / R8 rules
# Covers every library in app/build.gradle.kts
# ════════════════════════════════════════════════════════════════════════════

# ── Debugging aids ───────────────────────────────────────────────────────────
# Keep original source-file names and line numbers in crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.coroutines.** { volatile <fields>; }
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlin.**

# ── AndroidX / Jetpack ───────────────────────────────────────────────────────
-keep class androidx.** { *; }
-dontwarn androidx.**

# ── Hilt / Dagger ────────────────────────────────────────────────────────────
# Keep generated Hilt components, entry points, and inject targets.
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.HiltAndroidApp class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.AndroidEntryPoint class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.DefineComponent class *
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keepclasseswithmembernames class * { @dagger.** <methods>; }
-keepclasseswithmembernames class * { @javax.inject.** <fields>; }
-keepclasseswithmembernames class * { @javax.inject.** <methods>; }
-dontwarn dagger.**
-dontwarn javax.inject.**

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ── Retrofit & OkHttp ────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Gson ─────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Glide ────────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Lottie ───────────────────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── MPAndroidChart ───────────────────────────────────────────────────────────
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ── ExoPlayer (Media3) ────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn androidx.media3.**
-dontwarn com.google.android.exoplayer2.**

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# ── Navigation SafeArgs (Parcelable / Serializable) ──────────────────────────
-keepnames class * implements android.os.Parcelable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ── Paging ────────────────────────────────────────────────────────────────────
-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**

# ── DataStore ─────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Security Crypto / BouncyCastle ───────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn androidx.security.crypto.**

# ── Google Maps & CameraX ────────────────────────────────────────────────────
-keep class com.google.maps.android.** { *; }
-keep class androidx.camera.** { *; }
-dontwarn com.google.maps.android.**
-dontwarn androidx.camera.**

# ── Google Drive / API client ────────────────────────────────────────────────
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.**

# ── Generative AI (Gemini SDK) ───────────────────────────────────────────────
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ── PermissionX ──────────────────────────────────────────────────────────────
-keep class com.permissionx.** { *; }
-dontwarn com.permissionx.**

# ── Family4 app models (Room entities used via reflection by Gson/Room) ──────
-keep class com.family4.app.data.db.entity.** { *; }
-keep class com.family4.app.data.model.** { *; }

# ── ViewBinding (no obfuscation needed, just don't strip) ────────────────────
-keepclassmembers class ** implements androidx.viewbinding.ViewBinding {
    public static ** inflate(android.view.LayoutInflater);
    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
