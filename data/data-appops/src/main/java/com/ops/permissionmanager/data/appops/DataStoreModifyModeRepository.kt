package com.ops.permissionmanager.data.appops

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ops.permissionmanager.core.model.ModifyMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 DataStore（替代 SharedPreferences）的修改模式仓库。
 *
 * 持久化用户选择的 [ModifyMode]，并以 StateFlow 暴露供界面观察。
 * 启动竞态防护：构造时同步读取持久化值（DataStore 首次读取为磁盘 IO，
 * 通常在毫秒级），避免 Router 在读取完成前按默认 AUTO 路由。
 */
@Singleton
class DataStoreModifyModeRepository @Inject constructor(
    @ApplicationContext context: Context
) : ModifyModeRepository {

    companion object {
        /** DataStore 中保存修改模式的键。 */
        val KEY_MODIFY_MODE = stringPreferencesKey("modify_mode")
    }

    private val dataStore = context.opsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _modifyMode = MutableStateFlow(
        runBlocking(Dispatchers.IO) {
            ModifyMode.fromName(dataStore.data.first()[KEY_MODIFY_MODE])
        }
    )

    override val modifyMode: StateFlow<ModifyMode> = _modifyMode.asStateFlow()

    override fun setModifyMode(mode: ModifyMode) {
        _modifyMode.value = mode
        scope.launch {
            dataStore.edit { it[KEY_MODIFY_MODE] = mode.name }
        }
    }
}