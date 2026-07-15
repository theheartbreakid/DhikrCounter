package com.Crescent.DhikrCounter.ui.components.catalog.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap {
    return Bitmap.createScaledBitmap(asAndroidBitmap(), width, height, true).asImageBitmap()
}
