# Kotlin serialization
-keep @kotlinx.serialization.Serializable class * { *; }

# Hilt
-keep,allowobfuscation class dagger.hilt.** { *; }
-keep,allowobfuscation @dagger.hilt.android.HiltAndroidApp class * { *; }

# Navigation
-keep,allowobfuscation @androidx.navigation3.NavKey class * { *; }
