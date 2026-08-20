# 迭代日志（Iteration Log）

按 operit-vibe-coding 技能记录每轮增量：目标 → 验证 → 结果。

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