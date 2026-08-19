import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.ops.permissionmanager.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        // Macrobenchmark 通过 instrumentation 在设备上采集冷启动 / 帧率数据
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 被测应用：本仓库 app 模块
    targetProjectPath = ":app"

    // Macrobenchmark 模块需要以“自插桩”方式编译测试 APK
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.rules)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.junit)
}