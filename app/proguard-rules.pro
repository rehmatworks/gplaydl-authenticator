# kotlinx.serialization keeps its serializers in synthetic companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.gplaydl.authenticator.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.gplaydl.authenticator.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp ships optional platform integrations that R8 cannot see.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
