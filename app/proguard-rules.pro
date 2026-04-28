-keepattributes *Annotation*
-keepclassmembers class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}
