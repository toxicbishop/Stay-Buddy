package com.example.staybuddy.utils

object ImageUtils {
    /**
     * Injects Cloudinary optimization parameters into the URL.
     * This compresses the image, changes format to WebP/AVIF, and resizes it to a reasonable width,
     * drastically reducing loading times and bandwidth usage.
     */
    fun optimizeCloudinaryUrl(url: String?, width: Int = 800): String? {
        if (url == null) return null
        
        // Only optimize Cloudinary URLs that haven't been optimized yet
        if (!url.contains("res.cloudinary.com") || url.contains("q_auto")) {
            return url
        }

        // Replace the /upload/ path with the optimized path
        return url.replace("/upload/", "/upload/q_auto,f_auto,w_$width/")
    }

    /**
     * Compresses an image from a URI and returns a temporary file.
     * Scales down images larger than 1080p and applies JPEG compression.
     */
    fun compressImage(context: android.content.Context, uri: android.net.Uri): java.io.File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Scale down if larger than 1080p to save memory and bandwidth
            val maxDimension = 1080
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            // Create temp file
            val tempFile = java.io.File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(tempFile)
            
            // Compress to JPEG with 70% quality
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            
            outputStream.flush()
            outputStream.close()
            
            // Clean up bitmaps if a new one was created
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
