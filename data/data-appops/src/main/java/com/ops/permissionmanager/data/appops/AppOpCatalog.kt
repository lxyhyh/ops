package com.ops.permissionmanager.data.appops

import com.ops.permissionmanager.core.model.AppOp
import com.ops.permissionmanager.core.model.OpGroup

/**
 * 常见 AppOps 操作清单。
 * 名称对应 cmd appops 命令中的 op 标识。
 */
object AppOpCatalog {

    private val catalog: List<AppOp> = listOf(
        // 后台运行
        AppOp("RUN_IN_BACKGROUND", "后台运行", OpGroup.BACKGROUND),
        AppOp("RUN_ANY_IN_BACKGROUND", "任意后台运行", OpGroup.BACKGROUND),
        AppOp("START_FOREGROUND", "启动前台服务", OpGroup.BACKGROUND),
        AppOp("FOREGROUND_SERVICE_TYPE", "前台服务类型", OpGroup.BACKGROUND),
        AppOp("SCHEDULE_EXACT_ALARM", "精确闹钟", OpGroup.BACKGROUND),
        AppOp("USE_ALARMS", "使用闹钟", OpGroup.BACKGROUND),
        // 隐私
        AppOp("READ_CLIPBOARD", "读取剪贴板", OpGroup.PRIVACY),
        AppOp("GET_USAGE_STATS", "读取应用使用情况", OpGroup.PRIVACY),
        AppOp("ACCESS_FINE_LOCATION", "精确定位", OpGroup.PRIVACY),
        AppOp("ACCESS_COARSE_LOCATION", "粗略定位", OpGroup.PRIVACY),
        AppOp("READ_CONTACTS", "读取联系人", OpGroup.PRIVACY),
        AppOp("READ_CALL_LOG", "读取通话记录", OpGroup.PRIVACY),
        AppOp("READ_SMS", "读取短信", OpGroup.PRIVACY),
        AppOp("READ_PHONE_STATE", "读取手机状态", OpGroup.PRIVACY),
        AppOp("READ_PHONE_NUMBERS", "读取手机号码", OpGroup.PRIVACY),
        AppOp("READ_DEVICE_IDENTIFIERS", "读取设备标识", OpGroup.PRIVACY),
        AppOp("READ_ICC_SMS", "读取 SIM 短信", OpGroup.PRIVACY),
        AppOp("READ_WIFI_STATE", "读取 WiFi 状态", OpGroup.PRIVACY),
        AppOp("QUERY_ALL_PACKAGES", "查询所有应用", OpGroup.PRIVACY),
        // 通知
        AppOp("POST_NOTIFICATION", "发送通知", OpGroup.NOTIFICATION),
        AppOp("READ_NOTIFICATIONS", "读取通知", OpGroup.NOTIFICATION),
        AppOp("ACCESS_NOTIFICATIONS", "访问通知", OpGroup.NOTIFICATION),
        // 传感器
        AppOp("CAMERA", "相机", OpGroup.SENSOR),
        AppOp("RECORD_AUDIO", "录音", OpGroup.SENSOR),
        AppOp("BODY_SENSORS", "身体传感器", OpGroup.SENSOR),
        AppOp("VIBRATE", "振动", OpGroup.SENSOR),
        AppOp("USE_SIP", "使用 SIP", OpGroup.SENSOR),
        // 电量
        AppOp("WAKE_LOCK", "唤醒锁", OpGroup.BATTERY),
        AppOp("KEEP_SCREEN_ON", "保持屏幕常亮", OpGroup.BATTERY),
        AppOp("RUN_IN_BACKGROUND_WHITELIST", "后台运行白名单", OpGroup.BATTERY),
        AppOp("CHANGE_WIFI_STATE", "修改 WiFi 状态", OpGroup.BATTERY),
        AppOp("CHANGE_NETWORK_STATE", "修改网络状态", OpGroup.BATTERY),
        // 存储
        AppOp("READ_EXTERNAL_STORAGE", "读取存储", OpGroup.STORAGE),
        AppOp("WRITE_EXTERNAL_STORAGE", "写入存储", OpGroup.STORAGE),
        AppOp("READ_MEDIA_IMAGES", "读取图片", OpGroup.STORAGE),
        AppOp("READ_MEDIA_VIDEO", "读取视频", OpGroup.STORAGE),
        AppOp("READ_MEDIA_AUDIO", "读取音频", OpGroup.STORAGE),
        // 其他
        AppOp("SYSTEM_ALERT_WINDOW", "悬浮窗", OpGroup.OTHER),
        AppOp("WRITE_SETTINGS", "修改系统设置", OpGroup.OTHER),
        AppOp("REQUEST_INSTALL_PACKAGES", "安装未知应用", OpGroup.OTHER),
        AppOp("INTERACT_ACROSS_USERS", "跨用户交互", OpGroup.OTHER),
        AppOp("MANAGE_EXTERNAL_STORAGE", "管理所有文件", OpGroup.OTHER)
    )

    private val byName: Map<String, AppOp> = catalog.associateBy { it.name }

    fun find(name: String): AppOp? = byName[name]

    fun all(): List<AppOp> = catalog
}
