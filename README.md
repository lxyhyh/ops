# OPS 权限管家

管理 Android 应用 AppOps 权限的桌面级工具，支持查看、修改、批量管理应用权限，以及查看权限使用历史。支持 **Root** 或 **ADB (Shizuku)** 两种权限修改方式。

## 功能特性

- **应用权限清单**：查看所有已安装应用的 AppOps 权限状态（允许 / 拒绝 / 询问 / 默认），按权限组分类展示，支持 all / 系统 / 用户应用过滤
- **修改单个权限**：一键切换应用的任意权限状态，即时生效
- **批量管理**：一次对多个应用执行同一权限操作，支持进度显示、结果列表与取消
- **权限使用历史**：查看应用在何时使用了哪些权限（基于系统记录）
- **多种修改方式**：自动 / Root（`su`）/ ADB (Shizuku)，自动模式智能选择当前可用方式
- **权限目录完善**：内置 AOSP 完整 AppOps 权限清单（200+ 项），系统返回的所有权限都能正确识别和展示
- **主题设置**：跟随系统 / 浅色 / 深色主题可切换

## 环境要求

| 项目 | 要求 |
| --- | --- |
| 系统版本 | Android 10（API 29）及以上 |
| 权限 | 需 Root，或通过 Shizuku 授予 ADB 权限 |
| 设备 | Android 手机 / 平板 |

> 本工具通过 `cmd appops` 和 `dumpsys appops` 系统命令读取和修改权限，因此需要足够权限（root 或 Shizuku/ADB）才能运行。

## 构建方法

需要 JDK 25 和 Android SDK 37。

```bash
# 编译正式版 APK
./gradlew assembleRelease

# 运行单元测试
./gradlew testDebugUnitTest
```

编译产物位于 `app/build/outputs/apk/release/app-release.apk`。

也可以直接使用 GitHub Actions 的 CI 构建产物（提交到 `main` 分支后自动构建）。

## 使用方法

1. 安装 APK 后打开应用
2. 首次使用会检测权限环境（Root / Shizuku），无可用方式时会显示引导说明
3. 在应用列表中选择要管理的应用
4. 查看权限状态，点击即可修改（允许 / 拒绝 / 询问 / 默认）
5. 使用"批量管理"可一次对多个应用执行同一操作
6. 在"历史记录"页查看权限使用情况
7. 在"设置"页切换修改方式与主题
   - 自动：自动选择当前可用的方式（Root 优先，否则 Shizuku）
   - Root：通过 `su` 在管理员身份下执行命令
   - ADB (Shizuku)：需先在 Shizuku 中授权

## 风险提示

> ⚠️ 修改系统权限可能导致应用异常、数据丢失或系统不稳定。请谨慎操作，建议只修改你了解其用途的权限。

## 技术架构

多模块 Android 工程，采用 MVVM + Jetpack Compose + Hilt 依赖注入：

```
app                  # 应用入口
├── core
│   ├── core-model   # 领域模型
│   └── core-ui      # 通用 UI 组件
├── data
│   ├── data-appops  # AppOps 数据层（命令执行 + 输出解析）
│   └── data-applist # 应用列表数据层
└── feature
    ├── feature-applist  # 应用列表页 + 详情页
    ├── feature-batch    # 批量管理页
    ├── feature-history  # 权限历史页
    └── feature-settings # 设置页 / 根引导
```

核心设计：

- **AppOpsRepository**：核心深模块，封装命令构造、版本差异与错误处理
- **AppOpsParser**：纯函数解析器，负责解析 `cmd appops` 输出，便于单元测试
- **CommandExecutor / Router**：命令执行抽象与路由层，支持 Root（`su`）与 Shizuku 两种实现，并带可用性缓存，自动模式据此选择执行器
- **ProcessRunner**：统一执行外部进程，含超时强杀与 stdout/stderr 汇总

## 相关文档
- [DESIGN.md](DESIGN.md) — 技术设计文档
- [CHANGELOG.md](CHANGELOG.md) — 更新日志
- [PRIVACY.md](PRIVACY.md) — 隐私说明
- [docs/BENCHMARKS.md](docs/BENCHMARKS.md) — 性能门禁与基线
- [docs/REUSE-DECISIONS.md](docs/REUSE-DECISIONS.md) — 复用决策记录

## 许可证
本项目采用 [Apache License 2.0](LICENSE)。

本项目仅供学习与技术研究使用。
