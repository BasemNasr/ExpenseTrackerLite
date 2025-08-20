package com.bn.bassemexpensetrackerlite.di

import android.content.Context
import androidx.room.Room
import com.bn.bassemexpensetrackerlite.data.local.AppDatabase
import com.bn.bassemexpensetrackerlite.data.remote.ExchangeRateApi
import com.bn.bassemexpensetrackerlite.data.export.ExportService
import com.bn.bassemexpensetrackerlite.data.export.ExportServiceImpl
import com.bn.bassemexpensetrackerlite.data.repository.ExpenseRepositoryImpl
import com.bn.bassemexpensetrackerlite.domain.usecase.ConvertToUsdUseCase
import com.bn.bassemexpensetrackerlite.domain.repository.ExpenseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "expense_db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideExchangeApi(client: OkHttpClient, moshi: Moshi): ExchangeRateApi = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ExchangeRateApi::class.java)

    @Provides
    @Singleton
    fun provideRepository(db: AppDatabase, api: ExchangeRateApi): ExpenseRepository = ExpenseRepositoryImpl(
        expenseDao = db.expenseDao(),
        exchangeRateApi = api
    )

    @Provides
    @Singleton
    fun provideConvertToUsdUseCase(repository: ExpenseRepository): ConvertToUsdUseCase = ConvertToUsdUseCase(repository)

    @Provides
    @Singleton
    fun provideExportService(): ExportService = ExportServiceImpl()
}


