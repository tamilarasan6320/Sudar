package com.sudar.tnpscapp.nativeapp.core.services

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for providing service dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ServicesModule {

    @Provides
    @Singleton
    fun provideOneSignalService(
        @ApplicationContext context: Context
    ): OneSignalService {
        return OneSignalService(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseService(
        @ApplicationContext context: Context
    ): FirebaseService {
        return FirebaseService(context)
    }

    @Provides
    @Singleton
    fun provideTruecallerService(): TruecallerService {
        return TruecallerService()
    }

    @Provides
    @Singleton
    fun provideUpdateService(
        @ApplicationContext context: Context
    ): UpdateService {
        return UpdateService(context)
    }

    @Provides
    @Singleton
    fun provideSmsRetrieverService(
        @ApplicationContext context: Context
    ): SmsRetrieverService {
        return SmsRetrieverService(context)
    }
}
