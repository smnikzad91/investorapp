# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class ir.devtrader.investor.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class ir.devtrader.investor.data.remote.dto.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
