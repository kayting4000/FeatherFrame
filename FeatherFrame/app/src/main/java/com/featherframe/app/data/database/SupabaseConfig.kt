package com.featherframe.app.data.database

/**
 * Centralized Supabase configuration.
 * In production, read these from BuildConfig or environment variables.
 */
object SupabaseConfig {
    // ============================================================
    // ⚠️  REPLACE THESE WITH YOUR ACTUAL SUPABASE PROJECT CREDENTIALS
    // ============================================================

    const val SUPABASE_URL = "https://YOUR_SUPABASE_PROJECT.supabase.co"
    const val SUPABASE_REST_URL = "$SUPABASE_URL/rest/v1/"
    const val SUPABASE_AUTH_URL = "$SUPABASE_URL/auth/v1/"
    const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"

    // Google Drive OAuth 2.0
    const val GOOGLE_CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com"
    const val GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    // Supabase tables
    const val TABLE_PHOTOGRAPHERS = "photographers"
    const val TABLE_BIRDS = "birds"
    const val TABLE_BIRD_CAPTURES = "bird_captures"
    const val TABLE_CAPTURE_LIKES = "capture_likes"
    const val TABLE_CAPTURE_COMMENTS = "capture_comments"
}
