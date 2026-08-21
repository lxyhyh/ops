# 性能基线（BENCHMARKS）

> 本应用为侧载场景（不走 Google Play），性能验证以**本机手动门禁**为主：发布前跑一次 Macrobenchmark，记录基线并对照回归。

## 运行方式（手动门禁）

```bash
# 前置：连接已安装 debug 版应用的真实设备/模拟器，且已开启开发者选项
./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=DEBUGGABLE
```

或通过 Android Studio 运行 `benchmark` 模块的 instrumentation 测试。

## 已有基准

| 基准 | 位置 | 说明 |
|---|---|---|
| 冷启动（Cold Start） | `benchmark/.../StartupBenchmark.coldStart` | 进程全新拉起至首帧可交互耗时 |
| 热启动（Warm Start） | `benchmark/.../StartupBenchmark.warmStart` | 进程存活时前台启动耗时 |
| 应用列表滚动 | `benchmark/.../AppListScrollBenchmark.scrollAppList` | 应用列表滚动帧率/卡顿 |

## 基线记录（本机 2026-08-21 设备待首次运行后填写）

| 基准 | 基线值 | 日期 | 备注 |
|---|---|---|---|
| Cold Start | 待记录 | - | - |
| Warm Start | 待记录 | - | - |
| AppList Scroll | 待记录 | - | - |

> 规则：发布前必须执行一次并核对与上一条基线无显著回退（冷启动 > 200ms 增量视为需排查）。

## 相关优化记录

- 2026-08-20：应用列表/批量页改为「磁盘缓存秒开 + 后台刷新」，冷启动不再全量遍历 PackageManager。
- 2026-08-21：磁盘缓存增加 10 分钟过期失效（防安装/卸载后列表陈旧）。
- 2026-08-21：详情页并行拉取权限状态、诊断信息与审计记录（`coroutineScope + async`）。
