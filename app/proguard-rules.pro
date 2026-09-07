# ProGuard rules for Family4

# Hilt
-keepclasseswithmembernames class * { @dagger.hilt.* <fields>; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Retrofit & OkHttp
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Google Maps & Location
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# CameraX
-keep class androidx.camera.** { *; }

# Coil
-keep class coil.** { *; }

# Navigation safe args
-keepnames class * implements android.os.Parcelable
-keepnames class * implements java.io.Serializable

# General
-keepattributes SourceFile,LineNumberTable
-dontobfuscate
