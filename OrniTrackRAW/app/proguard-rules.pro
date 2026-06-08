# Keep Room entities
-keep class com.ornitrack.raw.data.database.** { *; }

# Keep Google Drive API models
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.services.drive.**

# Keep TensorFlow Lite model
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**