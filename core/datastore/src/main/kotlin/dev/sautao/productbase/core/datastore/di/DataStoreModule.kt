package dev.sautao.productbase.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.common.log.Logger
import dev.sautao.productbase.core.datastore.AppPreferences
import dev.sautao.productbase.core.datastore.DataStoreAppPreferences
import javax.inject.Singleton

private const val PREFERENCES_FILE = "app_preferences"
private const val TAG = "DataStore"

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreProvidesModule {
    /**
     * A corrupt file is replaced with empty preferences rather than crashing the app on start.
     * The user loses their settings; they do not lose the app.
     */
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        logger: Logger,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler =
            ReplaceFileCorruptionHandler {
                logger.e(TAG, "Preferences file was corrupt and has been reset", it)
                emptyPreferences()
            },
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE) },
        )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreBindsModule {
    @Binds
    @Singleton
    abstract fun bindAppPreferences(impl: DataStoreAppPreferences): AppPreferences
}
