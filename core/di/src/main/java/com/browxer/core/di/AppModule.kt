package com.browxer.core.di

import android.content.Context
import androidx.room.Room
import com.browxer.core.database.BrowxerDatabase
import com.browxer.core.database.DownloadDao
import com.browxer.engine.resolver.DirectMediaResolver
import com.browxer.engine.resolver.DashParser
import com.browxer.engine.resolver.HlsParser
import com.browxer.engine.resolver.YtdlpResolver
import com.browxer.engine.sniffer.MediaSnifferBridge
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

    @Provides @Singleton
    fun provideSnifferScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun provideSnifferBridge(scope: CoroutineScope): MediaSnifferBridge =
        MediaSnifferBridge(scope)

    @Provides @Singleton
    fun provideHlsParser(http: OkHttpClient): HlsParser = HlsParser(http)

    @Provides @Singleton
    fun provideDashParser(http: OkHttpClient): DashParser = DashParser(http)

    @Provides @Singleton
    fun provideDirectResolver(http: OkHttpClient): DirectMediaResolver =
        DirectMediaResolver(http)

    @Provides @Singleton
    fun provideYtdlpResolver(): YtdlpResolver = YtdlpResolver()
}
