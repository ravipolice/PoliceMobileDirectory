package com.example.policemobiledirectory.di

import com.example.policemobiledirectory.BuildConfig
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.api.EmployeeApiService
import com.example.policemobiledirectory.api.OfficersSyncApiService
import com.example.policemobiledirectory.api.SyncApiService
import com.example.policemobiledirectory.api.UsefulLinksApiService
import com.example.policemobiledirectory.data.remote.DocumentsApiService
import com.example.policemobiledirectory.data.remote.GalleryApiService
import com.example.policemobiledirectory.repository.DocumentsRepository
import com.example.policemobiledirectory.repository.GalleryRepository
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // --- Media (Gallery & Documents) ---

    @Provides
    @Singleton
    @Named("GalleryRetrofit")
    fun provideGalleryRetrofit(loggingInterceptor: HttpLoggingInterceptor): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()
        
        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.GALLERY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("DocumentsRetrofit")
    fun provideDocumentsRetrofit(loggingInterceptor: HttpLoggingInterceptor): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()
        
        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.DOCUMENTS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGalleryApiService(@Named("GalleryRetrofit") retrofit: Retrofit): GalleryApiService =
        retrofit.create(GalleryApiService::class.java)

    @Provides
    @Singleton
    fun provideDocumentsApiService(@Named("DocumentsRetrofit") retrofit: Retrofit): DocumentsApiService =
        retrofit.create(DocumentsApiService::class.java)

    // --- Employee & Sync ---

    @Provides
    @Singleton
    @Named("SyncRetrofit")
    fun provideSyncRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.EMPLOYEES_SYNC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("OfficersSyncRetrofit")
    fun provideOfficersSyncRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.OFFICERS_SYNC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideEmployeeApiService(@Named("SyncRetrofit") retrofit: Retrofit): EmployeeApiService =
        retrofit.create(EmployeeApiService::class.java)

    @Provides
    @Singleton
    fun provideSyncApiService(@Named("SyncRetrofit") retrofit: Retrofit): SyncApiService =
        retrofit.create(SyncApiService::class.java)

    @Provides
    @Singleton
    fun provideOfficersSyncApiService(@Named("OfficersSyncRetrofit") retrofit: Retrofit): OfficersSyncApiService =
        retrofit.create(OfficersSyncApiService::class.java)

    // --- Constants ---

    @Provides
    @Singleton
    @Named("ConstantsRetrofit")
    fun provideConstantsRetrofit(loggingInterceptor: HttpLoggingInterceptor): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.CONSTANTS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideConstantsApiService(@Named("ConstantsRetrofit") retrofit: Retrofit): ConstantsApiService =
        retrofit.create(ConstantsApiService::class.java)

    // --- Useful Links ---

    @Provides
    @Singleton
    @Named("UsefulLinksRetrofit")
    fun provideUsefulLinksRetrofit(loggingInterceptor: HttpLoggingInterceptor): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.USEFUL_LINKS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideUsefulLinksApiService(@Named("UsefulLinksRetrofit") retrofit: Retrofit): UsefulLinksApiService =
        retrofit.create(UsefulLinksApiService::class.java)

    // --- Missions Dashboard ---

    @Provides
    @Singleton
    @Named("MissionsRetrofit")
    fun provideMissionsRetrofit(loggingInterceptor: HttpLoggingInterceptor): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(BuildConfig.MISSIONS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMissionsApiService(@Named("MissionsRetrofit") retrofit: Retrofit): com.example.policemobiledirectory.api.MissionsApiService =
        retrofit.create(com.example.policemobiledirectory.api.MissionsApiService::class.java)
}
