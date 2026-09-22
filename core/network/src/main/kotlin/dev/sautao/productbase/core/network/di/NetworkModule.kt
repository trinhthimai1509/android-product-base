package dev.sautao.productbase.core.network.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.common.log.Logger
import dev.sautao.productbase.core.network.ConnectivityManagerNetworkMonitor
import dev.sautao.productbase.core.network.NetworkConfig
import dev.sautao.productbase.core.network.NetworkMonitor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton
import kotlin.time.toJavaDuration

/**
 * Wiring for the HTTP stack.
 *
 * The product must provide a [NetworkConfig]; nothing here reads `BuildConfig` or hardcodes a
 * host. A product talking to a second API adds its own qualified `@Provides` for that Retrofit
 * instance and reuses the [OkHttpClient] and [Json] provided here.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // Servers add fields; a new field should not break an installed app.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(config: NetworkConfig): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeout.toJavaDuration())
        .readTimeout(config.readTimeout.toJavaDuration())
        .writeTimeout(config.writeTimeout.toJavaDuration())
        .apply {
            if (config.enableHttpLogging) {
                // Added only when the product asks for it, so a release build cannot log bodies
                // even by accident.
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        config: NetworkConfig,
        client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
        logger: Logger,
    ): NetworkMonitor = ConnectivityManagerNetworkMonitor(context, logger)
}
