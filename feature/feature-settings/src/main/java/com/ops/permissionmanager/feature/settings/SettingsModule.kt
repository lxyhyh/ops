package com.ops.permissionmanager.feature.settings

import android.content.Context
import com.ops.permissionmanager.data.appops.ShizukuManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** feature-settings 的依赖装配：接口绑定 + Android 依赖的 seam 实现。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {

        @Provides
        @Singleton
        fun provideVersionNameProvider(
            @ApplicationContext context: Context
        ): VersionNameProvider = VersionNameProvider {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrDefault("").orEmpty()
        }

        @Provides
        @Singleton
        fun provideShizukuInstallDetector(
            @ApplicationContext context: Context
        ): ShizukuInstallDetector = ShizukuInstallDetector {
            runCatching {
                context.packageManager.getPackageInfo(ShizukuManager.SHIZUKU_PACKAGE, 0)
                true
            }.getOrDefault(false)
        }
    }
}