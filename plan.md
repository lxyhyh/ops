# OPS 权限管家 项目计划（plan.md）

> 单一真实来源：本文件是后续所有对话与代码的唯一依据。
> 最后更新：2026-08-17

## 目标

为已 root 的技术用户提供一个 Android 应用，用于查看、修改、批量管理应用的 AppOps 应用操作权限，并查看权限使用历史。

## 范围

**做：**
- 查看所有已安装应用的 AppOps 权限状态
- 修改单个应用的单个权限（允许 / 拒绝 / 询问 / 默认）
- 批量对多个应用执行同一权限操作
- 查看权限使用历史记录（预留实时监控扩展点）
- Root 环境检测与引导

**不做（防止蔓延）：**
- 实时权限监控（仅预留扩展点）
- 企业 MDM / 设备管理
- 卸载、冻结、清理应用
- iOS 或其他平台

## 规格

- 技术栈：Kotlin + Jetpack Compose（Material 3）+ MVVM + Coroutines/Flow
- 平台：Android 10+（minSdk 29）
- 运行环境：需要 root 权限
- 权限读写方式：root 执行 `cmd appops` 命令（兼容性优先）
- 工程结构：多模块（app / core-model / core-ui / data-appops / data-applist / feature-applist / feature-batch / feature-history）
- 依赖注入：Hilt
- 测试：核心解析逻辑纯函数单测 + Fake 适配器注入

## 里程碑

- M1（Issue 0）：多模块骨架 + Root 检测引导，项目可编译运行
- M2（Issue 1-2）：应用列表 + 权限查看 + 修改单个权限
- M3（Issue 3）：批量管理
- M4（Issue 4）：权限使用历史记录
- M5（Issue 5）：真机验证 + 文档 + 打包发布

## 关键决策记录

| 决策 | 结论 | 原因 |
|------|------|------|
| 权限类型 | AppOps 应用操作权限 | 用户确认 |
| 目标用户 | 会 root 的技术用户 | 用户确认 |
| 最低版本 | Android 10+ | 用户确认 |
| 审计深度 | 历史记录 + 预留扩展 | 用户确认 |
| 权限读写方式 | root 执行 cmd appops 命令 | 兼容性优于反射隐藏 API |
| 工程结构 | 多模块工程 | 用户确认，支持并行开发 |
