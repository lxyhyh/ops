package com.ops.permissionmanager.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 应用列表滚动帧率基准（FrameTimingMetric）。
 *
 * 运行：./gradlew :benchmark:connectedBenchmarkAndroidTest
 * 输出：FrameTimingMetric 的 frameDurationCpuMs 分布（p50/p90/p99）、jank 比例——
 * 对应“应用列表上下滑动卡”。
 */
@RunWith(AndroidJUnit4::class)
class AppListScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollAppList() = benchmarkRule.measureRepeated(
        packageName = "com.ops.permissionmanager",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        startActivityAndWait()
        // 等待应用列表首屏出现（底部导航上任意元素即认为已渲染）
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000)

        val w = device.displayWidth
        val h = device.displayHeight
        // 上下各滑动 3 屏，模拟用户快速浏览列表
        repeat(3) {
            device.swipe(w / 2, (h * 0.75).toInt(), w / 2, (h * 0.20).toInt(), 25)
        }
        repeat(3) {
            device.swipe(w / 2, (h * 0.20).toInt(), w / 2, (h * 0.75).toInt(), 25)
        }
    }
}