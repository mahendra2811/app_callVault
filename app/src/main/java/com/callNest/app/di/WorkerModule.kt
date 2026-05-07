package com.callNest.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt's `@HiltWorker` + `androidx-hilt-work` plugin wires
 * `HiltWorkerFactory` automatically into `SingletonComponent` — see
 * `CallNestApp.workManagerConfiguration`. This module is a placeholder for
 * any future custom worker dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule
