package com.zelgius.gateApp.service

import android.app.NotificationManager
import android.content.Context
import com.zelgius.gateApp.GateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackgroundModule {
    @Provides
    @Singleton
    fun work(@ApplicationContext context: Context, gateRepository: GateRepository, notificationManager: NotificationManager): Work =
        Work(context, gateRepository, notificationManager)
}