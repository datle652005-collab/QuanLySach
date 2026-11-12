// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // Thêm dòng này để Firebase có thể chạy được
    alias(libs.plugins.google.services) apply false

    // thêm dòng này để Hilt chạy được
    id("com.google.dagger.hilt.android") version "2.51" apply false
}