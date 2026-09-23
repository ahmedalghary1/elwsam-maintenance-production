-keepattributes Signature,*Annotation*,InnerClasses
-dontnote kotlinx.serialization.**

-keep class com.production.supervisor.data.remote.dto.** { *; }
-keep class com.production.supervisor.data.local.entity.** { *; }

-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}

-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
