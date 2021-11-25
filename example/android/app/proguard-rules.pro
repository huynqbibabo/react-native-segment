# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# These rules will ensure that our generated serializers dont get obfuscated
-keep,includedescriptorclasses class com.segment.analytics.kotlin.**$$serializer { *; }
-keepclassmembers class com.segment.analytics.kotlin.** {
    *** Companion;
}
-keepclasseswithmembers class com.segment.analytics.kotlin.** {
    kotlinx.serialization.KSerializer serializer(...);
}
