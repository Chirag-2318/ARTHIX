package com.chirag.arthix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Arthix Application — Hilt entry point.
 *
 * Annotated with [@HiltAndroidApp] to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 *
 * Registered in AndroidManifest.xml via android:name=".ArthixApplication".
 */
@HiltAndroidApp
class ArthixApplication : Application()
