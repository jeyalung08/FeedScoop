// FILE: build.gradle.kts  (Project: FeedScoop)
// Top-level build file — do NOT add dependencies here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.kapt)         apply false
    alias(libs.plugins.hilt.android)        apply false
    alias(libs.plugins.google.services)     apply false
}