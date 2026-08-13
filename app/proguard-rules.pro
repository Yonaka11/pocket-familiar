# Pocket Familiar ProGuard rules
# Add project-specific ProGuard rules here.

# Keep application class
-keep class com.mikazuki.pocketfamiliar.PocketFamiliarApplication

# Keep data model classes used with DataStore
-keep class com.mikazuki.pocketfamiliar.model.** { *; }

# Uncomment this to preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to hide the original source file name.
# -renamesourcefileattribute SourceFile
