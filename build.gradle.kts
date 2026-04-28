// Force JavaPoet 1.13.0 onto the Gradle buildscript classpath so the Hilt
// AggregateDepsTask worker (NoIsolation) picks it up instead of the older
// copy bundled inside AGP.
buildscript {
    configurations.classpath {
        resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    }
}

// Top-level build file — configuration that applies to all subprojects.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// Force a JavaPoet version that exposes canonicalName(), required by the Hilt Gradle plugin.
subprojects {
    configurations.all {
        resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    }
}
