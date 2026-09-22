package dev.sautao.productbase.core.notification.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.notification.ReminderScheduler
import dev.sautao.productbase.core.notification.internal.WorkManagerReminderScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NotificationProvidesModule {
    /**
     * WorkManager initialises itself through androidx.startup, so this only hands the existing
     * instance to Dagger. Nothing here requires a product to implement Configuration.Provider.
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationBindsModule {
    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
