package com.browxer.core.di

import android.content.Context
import androidx.room.Room
import com.browxer.core.database.BrowxerDatabase
import com.browxer.core.database.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Only bindings WITHOUT an @Inject constructor live here. Resolvers, parsers,
 * ranker, downloaders and the sniffer bridge all use @Inject constructors —
 * providing them here as well would be a duplicate-binding compile error.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): BrowxerDatabase =
        Room.databaseBuilder(ctx, BrowxerDatabase::class.java, "browxer.db")
            .fallbackToDestructiveMigration() // dev only — write migrations before release
            .build()

    @Provides
    fun provideDownloadDao(db: BrowxerDatabase): DownloadDao = db.downloadDao()

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Background scope for the sniffer bridge (Dispatchers.Default). */
    @Provides @Singleton
    fun provideSnifferScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
