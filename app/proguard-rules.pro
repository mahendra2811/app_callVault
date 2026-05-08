# --- Room ---
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keep,allowobfuscation,allowshrinking class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
-keep class hilt_aggregated_deps.** { *; }

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.callNest.app.**$$serializer { *; }
-keepclassmembers class com.callNest.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.callNest.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Apache POI (reflection-heavy) ---
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemasMicrosoftComOfficeOffice.** { *; }
-keep class schemasMicrosoftComOfficeExcel.** { *; }
-keep class schemasMicrosoftComVml.** { *; }
-keep class com.microsoft.schemas.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.stream.**

# --- iText ---
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# --- Tink ---
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.crypto.tink.proto.** { *; }
-dontwarn com.google.crypto.tink.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Compose ---
-keep class androidx.compose.runtime.** { *; }

# --- Project entities ---
-keep class com.callNest.app.data.local.entity.** { *; }

# --- Apache POI / log4j2 transitive references that don't ship on Android ---
-dontwarn org.osgi.**
-dontwarn aQute.bnd.annotation.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.brotli.dec.**
-dontwarn com.github.luben.zstd.**

# --- Sentry (auto-init disabled in manifest, but keep classes referenced via reflection) ---
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# --- Supabase / Ktor / OkHttp transitive optional deps ---
-dontwarn org.slf4j.**
-dontwarn javax.servlet.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- POI VML / extra Microsoft schema classes pruned out of poi-ooxml-lite ---
-dontwarn com.microsoft.schemas.**
-dontwarn com.graphbuilder.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
