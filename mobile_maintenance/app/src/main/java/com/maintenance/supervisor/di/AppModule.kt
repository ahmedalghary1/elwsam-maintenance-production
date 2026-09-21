package com.maintenance.supervisor.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.maintenance.supervisor.BuildConfig
import com.maintenance.supervisor.data.local.AppDatabase
import com.maintenance.supervisor.data.local.MIGRATION_1_2
import com.maintenance.supervisor.data.local.MIGRATION_2_3
import com.maintenance.supervisor.data.local.MIGRATION_3_4
import com.maintenance.supervisor.data.local.MIGRATION_4_5
import com.maintenance.supervisor.data.remote.*
import com.maintenance.supervisor.data.repository.AuthRepositoryImpl
import com.maintenance.supervisor.data.repository.MaintenanceRepositoryImpl
import com.maintenance.supervisor.domain.repository.AuthRepository
import com.maintenance.supervisor.domain.repository.MaintenanceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier annotation class RefreshClient

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
    @Provides @Singleton fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "maintenance.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    @Provides fun clock(): Clock = Clock.systemUTC()
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }
    @Provides @Singleton @RefreshClient fun refreshRetrofit(json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    @Provides @Singleton fun refreshApi(@RefreshClient retrofit: Retrofit): RefreshApi = retrofit.create(RefreshApi::class.java)
    @Provides @Singleton fun okHttp(bearer: BearerInterceptor, authenticator: TokenAuthenticator): OkHttpClient {
        val builder = OkHttpClient.Builder().addInterceptor(bearer).authenticator(authenticator)
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).writeTimeout(45, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) builder.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        return builder.build()
    }
    @Provides @Singleton fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL).client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    @Provides @Singleton fun api(retrofit: Retrofit): MaintenanceApi = retrofit.create(MaintenanceApi::class.java)
}

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun maintenance(impl: MaintenanceRepositoryImpl): MaintenanceRepository
}
