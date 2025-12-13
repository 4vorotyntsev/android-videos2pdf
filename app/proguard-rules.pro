# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.vs.videoscanpdf.data.entities.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# Media3
-keep class androidx.media3.** { *; }
