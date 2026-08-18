# 代码设计：OPS 权限管家（Android）

> 来源：PRD.md + ISSUES.md
> 日期：2026-08-17（v0.2 多模块版）；2026-08-19 同步对齐反编译后的执行层/解析层/批量实现描述
> 设计方法：深模块 + Seam 接缝 + 接口即测试面 + 多模块工程

---

## 一、设计原则（本项目的三条主线）

1. **深模块**：小接口背后承载大实现。界面层只学几个方法，就能获得完整的权限管理能力；复杂的命令构造、输出解析、版本适配全部藏在模块内部。
2. **正确 Seam**：把"命令执行"和"输出解析"放在干净的接缝处，测试穿过与调用者相同的接口。
3. **可测试性**：接受依赖不创建依赖、返回结果不产生副作用、小表面面积（更少方法 = 更少测试）。

---

## 二、多模块工程结构

```
OPS权限管家（根工程）
├── app/                      # 应用入口：Application、MainActivity、导航、依赖组装(Hilt)
├── core/
│   ├── core-model/           # 领域模型 + 统一错误模型（零依赖，被所有模块共享）
│   └── core-ui/              # 主题、通用 UI 组件
├── data/
│   ├── data-appops/          # ★权限数据核心★：AppOpsRepository + CommandExecutor 家族 + AppOpsParser
│   └── data-applist/         # 应用列表数据：AppListRepository
└── feature/
    ├── feature-applist/      # 应用列表 + 应用详情（查看 / 修改权限）
    ├── feature-batch/        # 批量管理
    └── feature-history/      # 权限使用历史记录
```

### 依赖规则（物理隔离，防止越界依赖）

| 模块 | 允许依赖 |
|------|---------|
| app | feature-*、core-ui、core-model |
| feature-* | data-*、core-ui、core-model |
| data-* | core-model |
| core-ui | core-model |
| core-model | 无（最底层） |

**关键约束**：
- feature 之间**互不依赖** → 可独立开发、独立编译（对应 Issue 并行）
- data 层不依赖任何 UI → 可单独测试
- core-model 零依赖 → 领域模型被所有模块共享，杜绝重复定义

### 依赖方向图（逻辑视图）

```
app（入口 + 导航）
   │
   ├──→ feature-applist ──→ data-appops ──→ core-model
   ├──→ feature-batch  ──→ data-appops ──→ core-model
   └──→ feature-history ──→ data-appops ──→ core-model
                    └──→ data-applist ──→ core-model
```

---

## 三、核心模块设计

### 3.1 AppOpsRepository（核心深模块，位于 data-appops）

**接口（小，3 个方法）：**
```kotlin
interface AppOpsRepository {
    suspend fun getAppOps(packageName: String): AppOpsState      // 获取某应用全部权限状态
    suspend fun setAppOp(packageName: String, op: AppOp, mode: OpMode): Result<Unit>  // 修改某权限
    suspend fun getHistory(): List<OpUsageRecord>                // 权限使用历史
}
```

**实现（大，藏在内部）：**
- 命令构造（`cmd appops get/set` 的拼装与包名校验，防命令注入）
- 调用 `CommandExecutor`（注入的命令通道）执行命令
- 调用 `AppOpsParser`（注入的解析器）解析输出
- 错误处理（统一映射为 core-model 的 AppOpsError）
- 并发控制与缓存（避免重复查询）

**为什么是深模块**：调用者只需学会 3 个方法签名，就能获得"查看 + 修改 + 审计"完整能力。删除它，这些复杂度会重新出现在每个 ViewModel 里 → 物有所值。

### 3.2 命令执行抽象（适配器 Seam，位于 data-appops）

**接口（小，2 个方法）：**
```kotlin
interface CommandExecutor {
    suspend fun execute(command: String): ShellResult   // 返回 stdout / stderr / exitCode
    suspend fun isAvailable(): Boolean                  // 该通道当前是否可用
}
```

**两个适配器（真实 Seam）：**
- `RootCommandExecutor`：su（Magisk / KernelSU / SuperSU 等不同 root 实现），超时控制 + 并发读流
- `ShizukuCommandExecutor`：反射 Shizuku API，异常统一映射为 `AppOpsError.CommandFailed`，不裸抛反射异常

**路由层 `CommandExecutorRouter`**：根据 `ModifyModeRepository` 的修改模式（AUTO / ROOT / SHIZUKU）在两者间选择。与反编译对齐：构造仅注入 root/shizuku 执行器与模式仓库（不持有 ShizukuManager）；root/shizuku 可用性各带 5s TTL 的 `AvailabilityCache`（无锁快速路径 + 锁内二次检查）；AUTO 兜底 RootExecutor；`isAvailable` 透传所选执行器。它同时实现 `ExecutionAvailability`，向上层解耦暴露"是否有可用通道"。

**Seam 说明**：测试注入 `FakeCommandExecutor`，生产注入真实实现，接口不变。

### 3.3 AppOpsParser（纯函数 Seam，★最重要测试面★，位于 data-appops）

**接口（小，2 个方法，类可注入）：**
```kotlin
class AppOpsParser @Inject constructor() {
    fun parseGetOutput(raw: String): List<AppOpState>        // 解析 cmd appops get 输出
    fun parseHistoryOutput(raw: String): List<OpUsageRecord> // 解析 dumpsys appops 输出
}
```

**实现（大）：**
- 不同 Android 版本（10 / 11 / 12 / 13+）输出格式差异适配
- 容错解析（跳过无法识别的行，不整体崩溃）
- 时间戳小数位不限（`\.\d+`，与反编译一致）；>3 位小数无法被 `[.SSS]` 格式器解析时跳过该条，不崩溃
- `parseGetOutput` 不去重（与原版一致，去重由 RealAppOpsRepository 按 op.name 完成）；`parseHistoryOutput` 逐条保留 Access/Reject 记录

**可注入化**：作为构造依赖注入到 `RealAppOpsRepository`，不依赖解析器内部的日期格式单例，便于测试注入真实实现。

**为什么最重要**：这是**纯函数**——不碰 root、不碰网络、无副作用。直接喂字符串、断言结果，是单元测试成本最低、收益最高的位置。命令输出格式差异是该项目最大的兼容性风险，全部集中在这里解决。

### 3.4 AppListRepository（位于 data-applist）

**接口（小）：**
```kotlin
interface AppListRepository {
    suspend fun getInstalledApps(): List<AppInfo>   // 应用名 / 图标 / 包名
}
```
**实现**：封装 PackageManager 查询。浅模块，但职责单一，独立成 data-applist 便于与权限逻辑解耦。

### 3.5 批量管理（位于 feature-batch）

批量逻辑内聚在 `BatchViewModel`：

```kotlin
class BatchViewModel @Inject constructor(
    private val appOpsRepository: AppOpsRepository,
    ...
) : ViewModel()
```

内部对选中应用逐个调用 `AppOpsRepository.setAppOp` 聚合进度与结果（进度 StateFlow + 结果列表），复用核心 seam，不重复实现命令逻辑。

---

## 四、领域模型与统一错误处理（位于 core-model）

**领域模型（零依赖，被所有模块共享）：**
- `AppOp`：AppOps 操作标识（后台运行、读取通知、剪贴板等）
- `OpMode`：权限模式（ALLOW / DENY / IGNORE / DEFAULT / ASK）
- `AppOpState`：某应用某权限的当前状态
- `AppOpsState`：某应用全部权限状态（按权限组分类）
- `OpUsageRecord`：单条权限使用记录（应用、权限、时间、次数）
- `AppInfo`：应用信息（名称、图标、包名）

**统一错误模型（实际实现）：**
```kotlin
sealed class AppOpsError : Exception() {
    data class CommandFailed(
        val exitCode: Int, val stderr: String = ""
    ) : AppOpsError()          // 命令执行失败（含 Shizuku 反射失败映射）
    data object InvalidPackage : AppOpsError()   // 包名非法/为空
}
```
界面层用 `runCatching`/异常统一处理，不在各 ViewModel 散落判断逻辑；协程内 `CancellationException` 一律重新抛出，不吞取消。

---

## 五、测试策略（接口即测试面）

| 测试目标 | 所在模块 | 穿过哪个 Seam | 依赖 |
|---------|---------|--------------|------|
| AppOpsParser 解析（核心） | data-appops | 纯函数直接调用 | 无（纯 JVM 单测） |
| AppOpsRepository 逻辑 | data-appops | 注入 Fake CommandExecutor（RecordingExecutor / ThrowingExecutor） | Fake CommandExecutor |
| 批量逻辑 | feature-batch | 注入 Fake AppOpsRepository | Fake Repository |
| ViewModel 状态流转 | 各 feature | 注入 Fake Repository | Fake Repository |
| Compose UI | 各 feature | 注入 Fake Repository | Robolectric / 模拟器 |

**关键点**：调用者和测试穿过**同一个**接口。生产代码用真实适配器，测试代码用假适配器，接口本身不改变。每个模块的测试放在模块内部，独立编译运行。

---

## 六、深模块分析小结

| 模块 | 接口大小 | 实现复杂度 | 类型 | 删除测试结论 |
|------|---------|-----------|------|-------------|
| AppOpsRepository | 3 方法 | 高（命令+解析+错误+版本） | 深模块 | 删除后复杂度散落到各 ViewModel → 物有所值 |
| AppOpsParser | 2 方法 | 高（版本适配） | 深模块 | 删除后解析逻辑散落 → 物有所值 |
| CommandExecutor / Router | 2 方法 / 多方法 | 中（su + Shizuku 适配 + 路由缓存） | 中等深度 | 保留 |
| AppListRepository | 1 方法 | 低 | 浅模块 | 职责单一，保留 |
| BatchViewModel | — | 中 | 中等深度 | 复用核心 seam，保留 |

---

## 七、与 Issue 切片的对应

| Issue | 涉及模块 |
|-------|---------|
| 0 项目骨架 + Root 检测 | 根工程搭建、app、core-model、core-ui、data-appops 骨架（CommandExecutor 家族 + Repository 接口） |
| 1 应用列表 + 权限查看 | feature-applist、data-applist、data-appops（Parser.parseGetOutput + getAppOps） |
| 2 修改单个权限 | feature-applist（详情页交互）、data-appops（setAppOp） |
| 3 批量管理 | feature-batch（BatchViewModel，复用 AppOpsRepository.setAppOp） |
| 4 历史记录 | feature-history、data-appops（Parser.parseHistoryOutput + getHistory） |
| 5 收尾 | 全模块真机验证 |

**并行开发说明**：Issue 1/2（feature-applist）、Issue 3（feature-batch）、Issue 4（feature-history）分属不同模块，可并行开发；它们只依赖 data 层和 core 层，互不阻塞。
