package com.example.staybuddy.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.room.Room
import com.example.staybuddy.data.local.StayBuddyDatabase
import com.example.staybuddy.data.local.PropertyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideNominatimService(): com.example.staybuddy.data.api.NominatimService {
        val interceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "StayBuddy/1.0 (contact@staybuddy.com)")
                    .build()
                chain.proceed(request)
            }
            .build()
            
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.staybuddy.data.api.NominatimService::class.java)
    }

    @Provides
    @Singleton
    fun providePhotonService(): com.example.staybuddy.data.api.PhotonService {
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "StayBuddy/1.0 (contact@staybuddy.com)")
                    .build()
                chain.proceed(request)
            }
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.staybuddy.data.api.PhotonService::class.java)
    }

    @Provides
    @Singleton
    fun provideMapboxGeocodingService(): com.example.staybuddy.data.api.MapboxGeocodingService {
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl("https://api.mapbox.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.staybuddy.data.api.MapboxGeocodingService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApi(): com.example.staybuddy.data.api.NotificationApi {
        val interceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
            
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://staybuddy-worker.aasavchauhan.workers.dev/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.staybuddy.data.api.NotificationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStayBuddyDatabase(
        @ApplicationContext context: Context
    ): StayBuddyDatabase = Room.databaseBuilder(
        context,
        StayBuddyDatabase::class.java,
        StayBuddyDatabase.DATABASE_NAME
    )
        .addMigrations(StayBuddyDatabase.MIGRATION_11_12)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun providePropertyDao(database: StayBuddyDatabase): PropertyDao {
        return database.propertyDao()
    }

    @Provides
    @Singleton
    fun provideSearchDao(database: StayBuddyDatabase): com.example.staybuddy.data.local.SearchDao {
        return database.searchDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: StayBuddyDatabase): com.example.staybuddy.data.local.FavoriteDao {
        return database.favoriteDao()
    }
    
    @Provides
    @Singleton
    fun provideChatDao(database: StayBuddyDatabase): com.example.staybuddy.data.local.ChatDao {
        return database.chatDao()
    }

    @Provides
    @Singleton
    fun provideRoommatePostDao(database: StayBuddyDatabase): com.example.staybuddy.data.local.RoommatePostDao {
        return database.roommatePostDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(database: StayBuddyDatabase): com.example.staybuddy.data.local.NotificationDao {
        return database.notificationDao()
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: StayBuddyDatabase): com.example.staybuddy.data.local.UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): com.google.android.gms.location.FusedLocationProviderClient {
        return com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
    }
}
