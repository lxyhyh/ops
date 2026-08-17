# OPS 权限管家

管理 Android 应用 AppOps 权限的桌面级工具，支持查看、修改、批量管理应用权限，以及查看权限使用历史。需要 **root 权限**。

## 功能特性

- **应用权限清单**：查看所有已安装应用的 AppOps 权限状态（允许 / 拒绝 / 询问 / 默认），按权限组分类展示
- **修改单个权限**：一键切换应用的任意权限状态，即时生效
- **批量管理**：一次对多个应用执行同一权限操作，支持进度显示与取消
- **权限使用历史**：查看应用在何时使用了哪些权限（基于系统记录）
- **完整权限识别**：内置 AOSP 完整 AppOps 权限清单（约 160 项），系统返回的所有权限都能正确识别和展示

## 环境要求

| 项目 | 要求 |
| --- | --- |
| 系统版本 | Android 10（API 29）及以上 |
| 权限 | 需要 root（通过 `su` 执行命令） |
| 设备 | 已 root 的 Android 手机 / 平板 |

> 本工具通过 `cmd appops` 和 `dumpsys appops` 系统命令读取和修改权限，因此必须拥有 root 权限才能运行。

## 构建方法

需要 JDK 17 和 Android SDK 36。

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
2. 首次使用会检测 root 环境，无 root 时会显示引导说明
3. 在应用列表中选择要管理的应用
4. 查看权限状态，点击即可修改（允许 / 拒绝 / 询问 / 默认）
5. 使用"批量管理"可一次对多个应用执行同一操作
6. 在"历史记录"页查看权限使用情况

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
    ├── feature-applist  # 应用列表页
    ├── feature-batch    # 批量管理页
    └── feature-history  # 权限历史页
```

核心设计：

- **AppOpsRepository**：核心深模块，封装命令构造、版本差异与错误处理
- **AppOpsParser**：纯函数解析器，负责解析 `cmd appops` 输出，便于单元测试
- **RootShell**：`su` 命令执行适配层，可在测试中替换为 Mock 实现

## 相关文档

- [PRD.md](PRD.md) — 产品需求文档
- [DESIGN.md](DESIGN.md) — 技术设计文档
- [ISSUES.md](ISSUES.md) — 开发任务拆分与进度
- [plan.md](plan.md) — 开发计划

## 许可证

本项目仅供学习与技术研究使用。
