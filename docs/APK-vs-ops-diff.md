# APK 反编译 ↔ ops 仓库源码差异分析报告

日期：2026-08-19
分析对象：`ops-debug.apk`（com.ops.permissionmanager v0.1.0 / versionCode 1，13 dex，未混淆）
对比基准：GitHub `lxyhyh/ops` 仓库（本地工作区 `ops/`，HEAD `8fbdf5a`）

## 对比方法

1. **文件级**：提取反编译类的 `compiled from: Xxx.kt` 元数据（47 个 Kotlin 源文件）与 ops 仓库 51 个主源码文件对比
2. **类级**：用 dex 解析器（Python）对比 13 个 dex 的完整类清单（target 28,072 类 / ops 构建 26,588 类）
3. **逻辑级**：抽查核心类（AppOpsParser / CommandExecutorRouter / RealAppOpsRepository / ShizukuManager / BatchScreen）

## 结论摘要

**ops 仓库源码已完整覆盖 APK 的功能，且经过后续重构演进（比 APK 更新）。**
APK 反编译中未发现 ops 缺失的"真实功能类"（差异类均为编译器生成类 `$` 内部类 / Hilt 生成类 / lambda 类命名差异）。

## 一、文件级差异

| 项目 | 结果 |
| --- | --- |
| 反编译源文件（compiled from） | 47 个 |
| ops 主源码文件 | 51 个（含 2 个测试） |
| APK 独有源文件 | **1 个：`OpSelector.kt`**（batch 模块，权限选择器） |
| ops 独有（APK 无同名） | AppFilter.kt / AppOpState.kt / ExecutionAvailability.kt / OpGroup.kt / SettingsUiState.kt / ShellResult.kt（均在反编译产物中有对应类，仅无 compiled from 标注）+ 2 个测试文件 |

## 二、类级差异（真实代码类，剔除编译器生成类）

### APK 独有真实类：0 个 ✅
APK 反编译中不存在 ops 缺失的功能类。

### ops 独有真实类（8 个，均为后续重构/演进产物）

| 类 | 说明 | 引入提交 |
| --- | --- | --- |
| `RootAvailability` | RootCheck 目标的可用性检查封装 | 重构提交 |
| `SettingsPrefKeys` | 设置项 SharedPreferences key 常量对象 | 重构提交 |
| `ExecutionAvailability` | 命令通道可用性接口（isAny/isRoot/isShizukuAvailable） | `1047b3a` |
| `LimitedInputStream` | ProcessRunner 输出流上限控制（防内存膨胀） | `1047b3a` |
| `AppOpsParser_Factory` | AppOpsParser 改为 @Inject 类（Hilt Factory） | `1047b3a` |
| `AppOpsModule_CommandExecutorsModule_*Factory` | AppOpsModule 拆分嵌套 CommandExecutorsModule（@Provides root/shizuku） | `38bdd8d` |
| `ShizukuCommandExecutorKt` | ShizukuCommandExecutor 顶层辅助函数（printlnIfBlank） | 演进 |

> 经 `git log -S` 核验：`ExecutionAvailability` / `LimitedInputStream` / `AppOpsParser @Inject` 均由提交
> `1047b3a refactor: 跨层解耦与健壮性优化（架构/审查/性能）` 引入。
> **即 ops 仓库（GitHub）版本已包含并超过 APK 版本。**

## 三、逻辑级抽查（均一致）

| 类 | 一致性 |
| --- | --- |
| `AppOpsParser` | 正则与解析逻辑一致（APK 为 object 单例，ops 重构为 @Inject class + Factory，行为等价） |
| `CommandExecutorRouter` | 字段/方法一致（APK 内联 ExecutionAvailability 方法，ops 抽接口使 CommandExecutorRouter 实现 ExecutionAvailability + AvailabilityCache TTL 5s） |
| `RealAppOpsRepository` | `cmd appops get/set` 命令构造、`dumpsys appops` 历史、包名校验（PACKAGE_NAME_REGEX）完全一致 |
| `ShizukuManager` | REQUEST_CODE=10001、SHIZUKU_PACKAGE、isBinderAvailable/isPermissionGranted 一致 |
| `BatchScreen` | Card 结构一致：SectionHeader("选择权限") + OpSelector + Divider + SectionHeader("目标状态") + FilterChip 模式选择 + 应用列表 + BatchFab |

## 四、已执行的补齐

| 变更 | 说明 |
| --- | --- |
| 新增 `feature/feature-batch/.../OpSelector.kt` | 将 APK 版独立的公开顶层函数 `OpSelector`（权限选择器对话框）补入 ops，与 APK 结构对齐 |
| 修改 `BatchRoute.kt` | 移除内嵌 private `OpSelector`，改由独立文件提供（同包引用，行为不变） |
| 构建验证 | `assembleDebug` 构建成功（JDK17 + AGP 8.11.1 + arm64 aapt2 override），APK 生成 |

## 五、说明与建议

1. **APK 未包含 ops 的后续重构**：APK（v0.1.0 debug）构建时间早于 GitHub 的重构提交
   （`38bdd8d` 模块化、`1047b3a` 跨层解耦等），因此 APK 实现更简洁（object 单例、无接口、无独立常量类）。
2. **若需将 ops 回退/对齐到 APK 结构**（不推荐）：需将 AppOpsParser 改回 object、删除 ExecutionAvailability/
   LimitedInputStream/SettingsPrefKeys/RootAvailability、合并 AppOpsModule 嵌套模块，属功能性回退。
3. **建议**：保留 ops 当前（更新的）实现；如需恢复的"丢失源码"有具体功能点，
   请提供线索（如某界面/功能/弹窗文案），可从反编译产物中精确比对恢复。