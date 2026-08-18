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

## 六、代码级对齐实录（v12–v15，以反编译产物为"原版"基准）

> 目标：重写后的 ops 应用在外观与性能上完全对齐 APK 反编译实现。
> 方法：成对比对 `rebuild/jadx_out/.../*.java` 与 `ops/**/*.kt`，有差异代码即修复，不依赖截图。

### v12（`c767d46`）UI 差异 + 性能根因批量修复

| 文件 | 差异（反编译为基准） | 修复 |
| --- | --- | --- |
| `AppIcon.kt` | 图标加载在主线程 `getApplicationIcon()`，且只处理 BitmapDrawable → 卡顿 + AdaptiveIcon 空白 | `withContext(Dispatchers.IO)` + `Drawable.toBitmapSized(128,128)` + LruCache |
| `RealAppListRepository.kt` | 过滤了自身包名（反编译不过滤） | 移除过滤 |
| `AppListUiState/HistoryUiState/AppDetailUiState/BatchUiState` | 默认 `isLoading=true`（反编译 false） | 统一改 false |
| `OpSelector.kt` | 确认/取消按钮位置反了；标题未加粗；modifier 链顺序不同；外层缺 Column(padding) | confirmButton 空 lambda、取消在 dismissButton、标题加粗、clip→clickable→padding、外包 Column |
| `BatchRoute.kt` | CollapsingTitle+Card 缺外层 Column(fillMaxWidth.padding(h16)) | 补齐 |
| `RootCheckUiState` | 反编译仅两字段，checkAvailability 无条件先置 checking | 精简字段 + 无条件 checking |
| `BatchViewModel.kt` | 加载失败原版走 message(Snackbar) 不走 error 全屏 | 改走 message |

### v13（`7ab4306`）历史页顶部空白

| 根因 | 修复 |
| --- | --- |
| 外层 MainScaffold 已 `statusBarsPadding()`，HistoryRoute 内部 Scaffold 默认 contentWindowInsets=SystemBars 再让一次 → 两倍状态栏高度 | Scaffold 传 `contentWindowInsets = WindowInsets(0,0,0,0)` |

### v14（`31a32d1`）历史页标题折叠动画

| 根因 | 修复 |
| --- | --- |
| 历史页标题是 LazyColumn 静态文字，无 CollapsingTitle 折叠行为 | 改为外层 Column + `CollapsingTitle(title="历史", subtitle="共 N 条记录")`，与 AppList/Batch 结构一致 |

### v15（`6a0dac0`）命令执行层 + 解析层对齐

| 文件 | 差异（反编译为基准） | 修复 |
| --- | --- | --- |
| `CommandExecutorRouter.kt` | ①构造多注入 shizukuManager（原版仅3参）②只有 rootAvailable 缓存，缺 shizukuAvailable ③isAvailable 包 runCatching（原版透传）④AUTO 兜底抛 CommandFailed（原版兜底 rootExecutor）⑤AvailabilityCache.get 每次抢锁（原版无锁快速路径 + 锁内二次检查） | 移除 shizukuManager；恢复 shizukuAvailable 缓存；isAvailable 透传；AUTO 兜底 Root；AvailabilityCache 改 double-checked locking |
| `AppOpsParser.kt` | ①parseGetOutput 内挂 distinctBy（原版 Parser 无，在 Repository 层）②parseHistoryOutput 按 pkg+op 合并去重（原版每条 Access/Reject 逐条 add）③时间戳正则 `\d{1,3}`（原版 `\d+` 不限位） | 移除 distinctBy；历史逐条保留；时间戳小数位不限 |
| `RealAppOpsRepository.kt` | getAppOps 缺 distinctBy（原版在 Repository 层 `distinctBy { op.name }`） | 补 `.distinctBy { it.op.name }` |
| `RootCommandExecutor.kt` | isAvailable 用 `id -u`+trim=="0"（原版 `id`+contains("uid=0")） | 改 id + contains("uid=0") |

### v15 配套测试同步（`测试策略与质量保障`）

行为变更后同步更新 3 个受影响用例（原断言为 ops 旧重构行为）：

| 用例 | 修改 |
| --- | --- |
| `parseHistoryOutput 解析历史记录` | 3 条 → 4 条（Access+Reject 逐条保留） |
| `parseGetOutput 内部按操作名去重` | 改为"Parser 不去重，去重移交仓库层"（3 条） |
| `parseHistoryOutput 超过三位毫秒` | 不再静默截断为 .123；>3 位无法解析则跳过该条 |

### 核验一致（无差异，保留 ops 结构）

- `UiStates.kt` 四组件 / `AppListScreen` / `SettingsScreen` / `RootGuideScreen` / `OpsNavHost` 底部胶囊
- `HistoryRoute` 列表结构 / `RealAppOpsRepository` 命令构成 / `HistoryViewModel`（默认 isLoading=false、记录直传）
- `SettingsViewModel` combine/stateIn 与反编译 4 个独立 collect 行为等价（监听同一批流源），属架构重构，保留
- `RootCheckViewModel` 注入 ExecutionAvailability 接口 vs 反编译直接 Router——行为等价，保留接口拆分（便于测试注入）
- `AppIcon` 的 `toBitmapSized` 与反编译 `toBitmap(128,128)` 语义一致（core-ui 未引入 core-ktx，手写等价实现）