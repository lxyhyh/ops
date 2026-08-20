plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ops.permissionmanager.data.appops"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation(libs.datastore.preferences)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
