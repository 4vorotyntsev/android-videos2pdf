package com.vs.videoscanpdf.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Additional app-level dependencies will be added here
    // Services like CameraService, FrameExtractor, PdfGenerator, etc.
}
