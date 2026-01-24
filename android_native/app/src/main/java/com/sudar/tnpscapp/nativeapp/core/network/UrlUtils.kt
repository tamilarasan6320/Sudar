package com.sudar.tnpscapp.nativeapp.core.network

import com.sudar.tnpscapp.nativeapp.BuildConfig

/**
 * Utility functions for URL resolution
 */
object UrlUtils {
    
    /**
     * Get the public base URL (without /api suffix) for serving static files
     */
    val publicBaseUrl: String
        get() {
            val base = BuildConfig.BASE_URL
            // Remove trailing /api/ or /api to get the public root
            return when {
                base.endsWith("/api/") -> base.dropLast(5)
                base.endsWith("/api") -> base.dropLast(4)
                base.endsWith("/") -> base.dropLast(1)
                else -> base
            }
        }

    /**
     * Resolve test category image URL from the API response
     * 
     * Handles various input formats:
     * - `uploads/test_categories/xxx.jpg` → full URL
     * - `test_category_xxx.jpg` (filename only) → full URL with path prefix
     * - `/uploads/test_categories/xxx.jpg` (leading slash) → normalized
     * - `null` or empty → returns null
     */
    fun resolveTestCategoryImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) {
            return null
        }

        var path = imagePath.trim()

        // Remove leading slash if present
        if (path.startsWith("/")) {
            path = path.substring(1)
        }

        // If it's just a filename (no directory), prefix with uploads/test_categories/
        if (!path.contains("/")) {
            path = "uploads/test_categories/$path"
        }

        // Build full URL
        return "$publicBaseUrl/$path"
    }

    /**
     * Resolve any image URL from the API
     */
    fun resolveImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) {
            return null
        }

        var path = imagePath.trim()

        // If already a full URL, return as-is
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }

        // Remove leading slash if present
        if (path.startsWith("/")) {
            path = path.substring(1)
        }

        // Build full URL
        return "$publicBaseUrl/$path"
    }
}
