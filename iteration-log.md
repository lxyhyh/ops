# 迭代日志（Iteration Log）

按 operit-vibe-coding 技能记录每轮增量：目标 → 验证 → 结果。

## 2026-08-22：性能优化专项（运行提速 + 资源消耗降低）

**目标**：识别并消除热点——启动/页面加载提速、减少 CPU/IO/内存浪费。

**热点识别（静态分析 + 代码证据）**：
1. 审计写入放大：`RealAuditRepository.recordChange` 每次修改全量解析+写文件，批量 100 项 → 100 次全量读写
2. 图标重复 IO：加载失败不入缓存，滚动回来重复 `getApplicationIcon`；同一包并发重组重复发起加载
3. `ProcessRunner` 单流上限 256KB：`dumpsys appops` 全量输出可能被截断（历史记录不全，正确性问题）
4. 历史页重复执行慢速 `dumpsys appops`（导航重建/重试场景）

**实施（单一变更、逐项验证）**：
- 审计写入合并：内存先行 + 500ms 延迟落盘合并窗口（批量执行 N 次全量写 → 1 次）
- 图标：失败缓存（`failedIcons` 集合）、进行中去重（`loadingIcons`）、LruCache 改 count-based 256 项（约 16MB 上限，避免缓存抖动）
- `ProcessRunner.MAX_READ_BYTES` 256KB → 4MB（历史完整性）
- `getHistory()` 内存缓存 TTL 60s（避免重复 dumpsys）
- 详情页修改权限/撤销后**局部更新**（`applyLocalUpdate` 就地改状态+刷新该权限审计，替代全量 load——省 3 次数据源查询且消除加载闪烁）

**验证**：全量 `testDebugUnitTest` 全绿（含新增缓存回归测试 `getHistory TTL 内二次调用走缓存`）✅ + `:app:assembleRelease` ✅ + apksigner v2 ✅

**产物**：`/sdcard/Download/OpsPermissionManager-MIUIX.apk`（4.17MB，v0.2.0）

**已评估并有意保留**（避免过度优化/破坏语义）：
- 批量执行保持串行（逐条结果/失败重试粒度；并发 root 命令有竞态风险）
- 批量进度更新保持全 state 更新（进度指示必需，LazyColumn 只重组可见项）
- 图标绘制尺寸保持 128×128（3x 密度下 42dp 显示的合理超采样）
- 应用列表 JSON 缓存用 org.json（平台内置，单次读写 <1ms）

## 2026-08-21：v0.2.0 功能体验增强 + 安全可靠性 + 发布治理（优化改进计划）

**目标**：按「优化改进计划」执行——功能与体验优先，本机验证为主，发布治理纳入。

**阶段一：功能与体验**
- 1.1 应用列表搜索过滤下沉 ViewModel（`filterApps` 纯函数可单测）、空结果态（区分无应用/无匹配+清除搜索）、过滤状态持久；新增 AppListViewModelTest 12 例
- 1.2 详情页诊断信息（版本/UID/目标SDK/启用/安装时间）：新增 `AppDetailInfo` + `getAppDetail` 按需查询，不影响列表秒开
- 1.3 批量页：执行前确认窗（权限→模式+目标预览+高危警示）、41 项高危 AppOps 分级（`AppOp.isHighRisk`，批量/详情页标注）、失败项单条重试（`BatchResultItem` 携带 op/mode）；新增 BatchViewModelTest 3 例

**阶段二：安全与可靠性**
- 2.1 权限修改审计：`AuditRecord` + `AuditRepository`（JSON 本地持久化，500 条上限），`setAppOp` 自动记录旧值/新值/通道；详情页「最近修改」展示 + 一键撤销；新增 RealAppOpsRepositoryTest 3 例
- 2.2 兼容性：历史解析保留 Access/Reject 类型、次数、UID（旧格式降级）；引导页区分 Root 可用 / Shizuku 未装 / 已装未授权；新增 AppOpsParserTest 2 例
- 2.3 缓存一致性：磁盘缓存 10 分钟过期失效

**阶段三：测试与性能门禁**
- 新增 AppDetailViewModelTest 7 例（加载/设置/撤销）
- docs/BENCHMARKS.md 性能门禁（Macrobenchmark 手动触发 + 基线记录规则）
- 说明：Settings/RealAppList 的 Robolectric 与 Shizuku 反射执行器 JVM 测试受 Android 运行时依赖限制，暂以本机验证覆盖（已记录）

**阶段四：发布治理**
- LICENSE（Apache-2.0）、PRIVACY.md、CHANGELOG.md
- 版本号 → v0.2.0（versionCode 2）
- CI：无 Secrets 时 artifact 后缀 `debug-signed`（防误用）；OSV 依赖漏洞扫描（报告不阻断）

**验证**：7 个模块 `testDebugUnitTest` 全绿 + `:app:assembleRelease` ✅ + apksigner v2 签名 ✅，提交 145f137..9997a20 全部推送，CI 自动触发。

**产物**：`/sdcard/Download/OpsPermissionManager-MIUIX.apk`（4.17MB，v0.2.0）

## 2026-08-21：去轮子化专项（复用优先审计）

**目标**：审计并消除项目中自造轮子，全面改用现成库/组件；修复批量页加载慢。

**能力点清单**：持久化、图标、图片缓存、弹窗/进度/按钮/顶栏 UI、Root 执行、列表加载性能。

**四层检查结论**：详见 [docs/REUSE-DECISIONS.md](docs/REUSE-DECISIONS.md)。

**实施**：
- 批量页/应用列表：磁盘缓存秒开 + 后台刷新（AppList/BatchViewModel）
- SharedPreferences → DataStore Preferences（含自动迁移，`Context.opsDataStore` 单例）
- 自绘 Tab 图标 → material-icons-extended（GridView/Tune/History）
- 自实现图标缓存 → Coil
- M3 ProgressIndicator/Snackbar/Button/AlertDialog → miuix 对应组件
- libsu 尝试：JitPack 401 拉不到 → 回滚保留原实现（记录原因）

**验证**：`:app:assembleRelease` ✅、`:feature:feature-batch:testDebugUnitTest` ✅、`:feature:feature-settings:testDebugUnitTest` ✅、apksigner 签名校验 ✅

**产物**：`/sdcard/Download/OpsPermissionManager-MIUIX.apk`（正式签名）

## 2026-08-20：MIUI X 改造收尾 + 设置页交互

- 设置页选择项：OverlayBottomSheet → OverlayListPopup 锚定小窗（右对齐箭头处弹出）
- 详情/历史/主界面 Scaffold/TopAppBar → miuix 版
- M3 AlertDialog → miuix OverlayDialog（批量选择权限、详情模式选择）
- 验证：release 构建 + 单测全绿，已推送

## 2026-08-20：MIUI X 配色修正

- 选中色回退静态初音绿（Monet 灰绿问题）
- ThemeController 补 isDark（深色跟随修复）
- 批量页未选中态主题色化（FilterChip 描边/Checkbox 描边）
- 应用列表项套 G2 卡片容器

## 2026-08-19：MIUI X 六批改造

1. 主题 Monet 初音绿 + 底栏强调色胶囊 + 设置页 ArrowPreference
2. 全站列表项 G2 squircle 圆角（9 处）
3. 详情/历史大卡片 → squircleBackground(20dp)
4. 批量配置大卡 → squircleBackground(20dp)
5. 搜索框胶囊 24dp + 底栏收紧（后按用户反馈恢复 48dp/12dp）
6. 设置页 3 卡 → MiuiShapes.squircle(20dp)

工具链：Kotlin 2.2.10 → 2.4.10、KSP 2.3.11、miuix 0.9.3、`android.builtInKotlin=false` 过渡通道。