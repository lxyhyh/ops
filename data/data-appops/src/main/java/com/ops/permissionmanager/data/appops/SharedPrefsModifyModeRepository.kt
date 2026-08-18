package com.ops.permissionmanager.data.appops

import android.content.Context
import com.ops.permissionmanager.core.model.ModifyMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 [android.content.SharedPreferences] 的修改模式仓库。
 *
 * 持久化用户选择的 [ModifyMode]，并以 StateFlow 暴露供界面观察。
 */
@Singleton
class SharedPrefsModifyModeRepository @Inject constructor(
    @ApplicationContext context: Context
) : ModifyModeRepository {

    companion object {
        /** SharedPreferences 中保存修改模式的键。 */
        const val KEY_MODIFY_MODE = "modify_mode"
    }

    private val prefs = context.getSharedPreferences("ops_settings", Context.MODE_PRIVATE)

    private val _modifyMode = MutableStateFlow(loadModifyMode())

    override val modifyMode: StateFlow<ModifyMode> = _modifyMode.asStateFlow()

    override fun setModifyMode(mode: ModifyMode) {
        prefs.edit().putString(KEY_MODIFY_MODE, mode.name).apply()
        _modifyMode.value = mode
    }

    /** 从 SharedPreferences 读取保存的模式，未找到时回退为 [ModifyMode.AUTO]。 */
    private fun loadModifyMode(): ModifyMode =
        ModifyMode.fromName(prefs.getString(KEY_MODIFY_MODE, null))
}