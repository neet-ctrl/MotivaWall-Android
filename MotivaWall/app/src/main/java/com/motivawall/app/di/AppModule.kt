package com.motivawall.app.di

import android.content.Context
import androidx.room.Room
import com.motivawall.app.data.MotivaDatabase
import com.motivawall.app.data.WallpaperDao
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
    fun provideDatabase(@ApplicationContext context: Context): MotivaDatabase =
        Room.databaseBuilder(context, MotivaDatabase::class.java, "motivawall.db")
            .addMigrations(MotivaDatabase.MIGRATION_1_2)
            .addMigrations(MotivaDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideWallpaperDao(database: MotivaDatabase): WallpaperDao = database.wallpaperDao()
}