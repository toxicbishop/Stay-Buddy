# Add project specific ProGuard rules here.

# Preserve generic signatures, inner classes & annotations for Gson, Cloudinary & Reflection
-keepattributes Signature,InnerClasses,EnclosingMethod,AnnotationDefault,*Annotation*

# Hilt & Dagger DI
-keep class dagger.hilt.** { *; }
-keep class * implements dagger.hilt.** { *; }
-keep class com.example.staybuddy.di.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Data Models, Room Entities & Repositories (for Firebase, Room & Location deserialization reflection)
-keep class com.example.staybuddy.data.model.** { *; }
-keepclassmembers class com.example.staybuddy.data.model.** { *; }
-keep class com.example.staybuddy.data.local.** { *; }
-keepclassmembers class com.example.staybuddy.data.local.** { *; }
-keep class com.example.staybuddy.data.repository.** { *; }
-keepclassmembers class com.example.staybuddy.data.repository.** { *; }

# Retrofit & OkHttp ProGuard rules
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class com.example.staybuddy.data.api.** { *; }
-keep interface com.example.staybuddy.data.api.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson rules
-keep class com.google.gson.** { *; }
-keepclassmembers class * implements com.google.gson.TypeAdapter { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Cloudinary
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# Mapbox ProGuard rules
-dontwarn com.tobrun.datacompat.annotation.**
-keep class com.mapbox.** { *; }
-keep interface com.mapbox.** { *; }
-dontwarn com.mapbox.**
-keepclasseswithmembernames class * {
    native <methods>;
}

# Firebase & Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Room & DataStore
-dontwarn androidx.room.**
-dontwarn androidx.datastore.**