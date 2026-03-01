package com.anomalyzed.docscanner.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.io.File

object BitmapUtils {

    /**
     * Decodes a large image file into a memory-efficient Bitmap using sub-sampling and RGB_565.
     */
    fun decodeSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return BitmapFactory.Options().run {
            // First decode with inJustDecodeBounds=true to check dimensions
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set and memory-efficient color config
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(file.absolutePath, this)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Converts a full-color bitmap to high-contrast black and white.
     */
    fun convertToBlackAndWhite(original: Bitmap): Bitmap {
        val w = original.width
        val h = original.height
        // RGB_565 is sufficient for B&W
        val bwBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bwBitmap)

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        
        // Optional: Increase contrast manually by modifying color matrix
        val contrast = 1.5f
        val translate = (-.5f * contrast + .5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return bwBitmap
    }
}
