# 复用决策记录（REUSE-DECISIONS）

> 按「复用优先」技能执行：写任何新代码前按 **平台能力 → 项目依赖 → 生态库 → 设计规范组件** 四层检查。
> 本文记录历次复用/自造决策，便于审查回溯。最后更新：2026-08-21。

## 检查顺序约定

1. **平台能力**：Operit 已激活包、Android 平台 API（PackageManager、org.json 等）
2. **项目现有依赖**：gradle/libs.versions.toml 中已有库
3. **生态成熟库**：主流方案（核对维护状态、许可证）
4. **设计规范组件**：DESIGN-SPEC.md / miuix 组件映射

---

## 决策清单

### D1. UI 框架层（已完成替换）
| 能力点 | 检查过程 | 采用方案 | 状态 |
|---|---|---|---|
| 主题系统 | miuix `MiuixTheme`+`ThemeController`（Monet 动态取色） | **现成**：miuix 0.9.3 | ✅ |
| 弹窗 | M3 AlertDialog → miuix `OverlayDialog`/`OverlayListPopup`/`OverlayBottomSheet` | **现成**：miuix overlay 系列 | ✅ |
| Scaffold/TopAppBar | M3 → miuix `Scaffold`/`TopAppBar` | **现成**：miuix basic | ✅ |
| 进度指示 | M3 CircularProgressIndicator → miuix `CircularProgressIndicator` | **现成**：miuix basic | ✅ |
| Snackbar | M3 → miuix `SnackbarHost`/`SnackbarHostState` | **现成**：miuix basic | ✅ |
| 按钮 | M3 Button → miuix `Button`（胶囊 cornerRadius） | **现成**：miuix basic | ✅ |
| 单选勾选 | M3 RadioButton → miuix `Checkbox` | **现成**：miuix basic | ✅ |
| 圆角 | RoundedCornerShape → miuix `squircleClip/squircleBackground` | **现成**：miuix-squircle | ✅ |
| 底栏 | 自绘悬浮胶囊（尺寸经用户确认） | **保留自造**：miuix FloatingNavigationBar 底部高度固定不可调，会破坏已确认外观（48dp 边距/距底 12dp） | 保留 |
| 筛选条 | M3 FilterChip | **保留**：miuix 无 FilterChip 组件 | 保留 |
| 文字标签 StatusChip/AppTypeLabel | 无对应（miuix Badge 为数字角标，语义不同） | **保留自造**：业务 UI 组件 | 保留 |

### D2. 持久化层（已完成替换）
| 能力点 | 检查过程 | 采用方案 | 状态 |
|---|---|---|---|
| 设置持久化（主题/修改方式） | SharedPreferences → **DataStore Preferences**（`Context.opsDataStore` 单例，含 SharedPreferencesMigration 自动迁移旧数据） | **现成**：androidx.datastore 1.1.7 | ✅ |
| 应用列表磁盘缓存 | org.json（Android 平台标准 API） | **保留**：平台能力层，kotlinx-serialization 需引入插件，收益小于成本 | 保留 |

### D3. 数据与图片层（已完成替换）
| 能力点 | 检查过程 | 采用方案 | 状态 |
|---|---|---|---|
| 应用图标加载缓存 | 自实现 LruCache+IO → **Coil**（`context.imageLoader` 解码缓存） | **现成**：io.coil-kt:coil-compose 2.7.0 | ✅ |
| Tab 图标 | 自绘 ImageVector（158 行 pathData）→ **material-icons-extended** 标准图标（R8 裁剪未用图标） | **现成**：androidx.compose.material 1.7.8 | ✅ |
| 应用列表构建 | 磁盘缓存 + 内存缓存（TTL 30s），冷启动秒开 | **保留自造**：无现成库覆盖"PackageManager 列表+label+排序"业务组合 | 保留 |

### D4. 命令执行层
| 能力点 | 检查过程 | 采用方案 | 状态 |
|---|---|---|---|
| Shizuku 执行 | `dev.rikka.shizuku:api` 官方 API | **现成**：shizuku-api 13.1.5 | ✅ |
| Root 执行 | 尝试 libsu 6.0.0（JitPack 会话复用）→ **网络环境 401 拉不到**，回滚保留原实现 | **保留自造**：ProcessRunner（超时 30s/流读取/进程销毁已完备），已记录对比 | 保留 |
| 命令输出解析 AppOpsParser | 业务解析逻辑 | **保留自造**：领域代码，无现成库 | 保留 |

### D5. 性能优化
| 能力点 | 检查过程 | 采用方案 | 状态 |
|---|---|---|---|
| 应用/批量列表秒开 | 磁盘缓存优先 + 后台刷新（AppList/BatchViewModel 统一） | **保留自造**：业务组合，无现成库 | ✅ |

---

## 自检（reuse-checklist）

- [x] 每个能力点均完成四层检查并有记录（见上表）
- [x] 引入新依赖均有理由（DataStore/Coil/icons-extended：标准方案替换自实现）
- [x] 未因"用库而用库"引入冗余依赖（libsu 拉不到即回滚，未强上）
- [x] 安全场景（Root/Shizuku 授权）使用官方 API（shizuku-api）
- [x] 自造项均已记录原因（底栏尺寸约束、FilterChip 无对应、业务解析、libsu 网络限制）
