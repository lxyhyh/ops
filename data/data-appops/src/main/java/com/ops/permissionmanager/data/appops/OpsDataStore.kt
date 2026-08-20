package com.ops.permissionmanager.data.appops

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore

/**
 * 全局共享的 DataStore 实例（进程内单例，按文件名唯一）。
 *
 * 替代 SharedPreferences 持久化用户设置（主题模式/修改方式等）。
 * 自动迁移旧 SharedPreferences("ops_settings") 中的历史数据。
 */
val Context.opsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ops_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "ops_settings"))
    }
)