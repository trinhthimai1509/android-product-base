package dev.sautao.productbase.demo.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.demo.data.DEMO_DATABASE_MIGRATIONS
import dev.sautao.productbase.demo.data.DemoDatabase
import dev.sautao.productbase.demo.data.SampleApi
import dev.sautao.productbase.demo.data.SampleEntryDao
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    /**
     * Note what is absent: no `fallbackToDestructiveMigration()`. A missing migration should stop
     * a developer, not wipe a user's data.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DemoDatabase =
        Room.databaseBuilder(context, DemoDatabase::class.java, "demo.db")
            .addMigrations(*DEMO_DATABASE_MIGRATIONS)
            .build()

    @Provides
    fun provideSampleEntryDao(database: DemoDatabase): SampleEntryDao = database.sampleEntryDao()

    /** Service interfaces belong to the product; `core:network` only supplies the Retrofit. */
    @Provides
    @Singleton
    fun provideSampleApi(retrofit: Retrofit): SampleApi = retrofit.create(SampleApi::class.java)
}
