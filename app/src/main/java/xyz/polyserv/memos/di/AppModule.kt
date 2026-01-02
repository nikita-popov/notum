package xyz.polyserv.memos.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import xyz.polyserv.memos.data.local.database.AppDatabase
import xyz.polyserv.memos.data.local.database.MemoDao
import xyz.polyserv.memos.data.local.database.SyncQueueDao
import xyz.polyserv.memos.data.remote.MemosApiService
import xyz.polyserv.memos.sync.NetworkConnectivityManager
import xyz.polyserv.memos.data.local.SharedPrefManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(
        sharedPrefManager: SharedPrefManager
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Auth interceptor
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            sharedPrefManager.getAccessToken()?.let { token ->
                if (token.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
            chain.proceed(requestBuilder.build())
        }

        // URL interceptor
        val hostSelectionInterceptor = Interceptor { chain ->
            var request = chain.request()
            val savedUrlString = sharedPrefManager.getServerUrl()

            val newBaseUrl = savedUrlString.toHttpUrlOrNull()

            if (newBaseUrl != null) {
                val newUrl = request.url.newBuilder()
                    .scheme(newBaseUrl.scheme)
                    .host(newBaseUrl.host)
                    .port(newBaseUrl.port)
                    .encodedPath("/api/v1" + request.url.encodedPath)
                    .build()

                request = request.newBuilder()
                    .url(newUrl)
                    .build()
            }
            chain.proceed(request)
        }

        // Keep order
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(hostSelectionInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        //val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Singleton
    @Provides
    fun provideMemosApiService(retrofit: Retrofit): MemosApiService =
        retrofit.create(MemosApiService::class.java)

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "memos.db"
        ).build()

    @Singleton
    @Provides
    fun provideNetworkConnectivityManager(
        @ApplicationContext context: Context
    ): NetworkConnectivityManager =
        NetworkConnectivityManager(context)

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Singleton
    @Provides
    fun provideMemoDao(database: AppDatabase): MemoDao =
        database.memoDao()

    @Singleton
    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao =
        database.syncQueueDao()
}
