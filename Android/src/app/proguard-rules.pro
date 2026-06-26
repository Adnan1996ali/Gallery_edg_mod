# Keep R classes (required for Compose/Material3 theme)
-keep class **.R
-keep class **.R$* { *; }

# Keep Compose Material3 theme classes
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep Hilt/Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep Moshi
-keep class com.squareup.moshi.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }

# Keep protobuf
-keep class com.google.protobuf.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# Keep LiteRT
-keep class com.google.ai.edge.litertlm.** { *; }

# Keep app model classes (used by Gson/Moshi serialization)
-keep class alpha.ai.chat.data.** { *; }
-keep class alpha.ai.chat.customtasks.common.** { *; }
