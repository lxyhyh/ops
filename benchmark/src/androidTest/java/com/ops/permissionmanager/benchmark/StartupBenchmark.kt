package com.ops.permissionmanager.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冷 / 热启动性能基准（Jetpack Macrobenchmark，Android 官方性能收集接口）。
 *
 * 运行：./gradlew :benchmark:connectedBenchmarkAndroidTest
 * 输出：StartupTimingMetric（firstFrame 到 content-drawn 的时间，可对比优化前后）。
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /** 冷启动：进程全新拉起，测量 App 首次可交互帧耗时（对应“刚进应用卡一下”）。 */
    @Test
    fun coldStart() = benchmarkRule.measureRepeated(
        packageName = "com.ops.permissionmanager",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }

    /** 热启动：进程存活，仅前台启动 Activity。 */
    @Test
    fun warmStart() = benchmarkRule.measureRepeated(
        packageName = "com.ops.permissionmanager",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM
    ) {
        startActivityAndWait()
    }
}