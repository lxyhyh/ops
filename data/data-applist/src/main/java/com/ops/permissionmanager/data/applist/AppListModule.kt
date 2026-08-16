package com.ops.permissionmanager.data.applist

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppListModule {

    @Binds
    @Singleton
    abstract fun bindAppListRepository(impl: RealAppListRepository): AppListRepository
}
