# 隐私说明（PRIVACY）

OPS 权限管家（OpsPermissionManager）**不联网、不收集、不上传任何个人数据**。所有数据仅存储在用户设备本地。

## 本应用存储的数据

| 数据 | 存储位置 | 用途 | 清除方式 |
|---|---|---|---|
| 主题模式 / 修改方式设置 | 本地 DataStore（`ops_settings`） | 记住用户偏好 | 卸载应用或清除应用数据 |
| 应用列表磁盘缓存 | `filesDir/ops_cache/app_list.json` | 冷启动秒开，减少 PackageManager 遍历 | 卸载应用或清除应用数据 |
| 权限修改审计记录 | `filesDir/ops_cache/audit.json` | 记录修改（旧值/新值/通道），支持撤销 | 卸载应用或清除应用数据 |

## 涉及的系统数据

- 通过 **Root（`su`）或 Shizuku（ADB 授权）** 执行 `cmd appops` / `dumpsys appops` 读取与修改 AppOps 权限状态。
- 应用列表信息（包名、名称、图标、是否系统应用、UID、版本等）仅来自本机 `PackageManager`，用于界面展示。

## 权限说明

- `QUERY_ALL_PACKAGES`：用于列出设备上已安装应用（权限管理工具必需）。
- `PACKAGE_USAGE_STATS`：用于读取权限使用历史（`dumpsys appops`）。

## 数据控制

- 本应用**不含任何网络权限**，不会将上述数据发送到任何服务器。
- 卸载应用后，上述本地数据全部删除。
- 审计记录为纯本地功能（撤销/查看），不对外共享。

## 更新

本说明随应用版本更新；重大变更会在 CHANGELOG 中说明。
