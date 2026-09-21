package com.production.supervisor.di

import android.content.Context
import androidx.room.Room
import com.production.supervisor.data.local.ProductionDatabase
import com.production.supervisor.data.local.dao.ProductionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ProductionDatabase {
        return Room.databaseBuilder(
            context,
            ProductionDatabase::class.java,
            "production_supervisor.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideProductionDao(db: ProductionDatabase): ProductionDao {
        return db.productionDao()
    }
}
