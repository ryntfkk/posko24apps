// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Mendefinisikan semua plugin via alias dari libs.versions.toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.hilt.android.gradle.plugin) apply false
}