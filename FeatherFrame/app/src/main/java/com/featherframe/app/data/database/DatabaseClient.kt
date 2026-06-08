package com.featherframe.app.data.database

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Retrofit client for Supabase/PostgreSQL REST API communication.
 * Uses centralized SupabaseConfig for endpoint URLs and keys.
 */
object DatabaseClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (SupabaseConfig.SUPABASE_URL.contains("YOUR_"))
            HttpLoggingInterceptor.Level.NONE
        else
            HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(SupabaseConfig.SUPABASE_REST_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val supabaseApi: SupabaseApi = retrofit.create(SupabaseApi::class.java)
}

interface SupabaseApi {

    // ============================================================
    // Auth
    // ============================================================

    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: Map<String, String>): Map<String, Any?>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(@Body body: Map<String, String>): Map<String, Any?>

    // ============================================================
    // Photographers
    // ============================================================

    @GET(SupabaseConfig.TABLE_PHOTOGRAPHERS)
    suspend fun getPhotographers(@Query("select") select: String = "*"): List<PhotographerDTO>

    @GET(SupabaseConfig.TABLE_PHOTOGRAPHERS)
    suspend fun getPhotographerById(
        @Query("photographer_id") id: String,
        @Query("select") select: String = "*"
    ): List<PhotographerDTO>

    @POST(SupabaseConfig.TABLE_PHOTOGRAPHERS)
    suspend fun createPhotographer(@Body photographer: PhotographerDTO): List<PhotographerDTO>

    @PATCH(SupabaseConfig.TABLE_PHOTOGRAPHERS)
    suspend fun updatePhotographer(
        @Query("photographer_id") id: String,
        @Body updates: Map<String, String?>
    )

    // ============================================================
    // Birds
    // ============================================================

    @GET(SupabaseConfig.TABLE_BIRDS)
    suspend fun getBirds(@Query("select") select: String = "*"): List<BirdDTO>

    @GET(SupabaseConfig.TABLE_BIRDS)
    suspend fun getBirdById(
        @Query("bird_id") id: String,
        @Query("select") select: String = "*"
    ): List<BirdDTO>

    @POST(SupabaseConfig.TABLE_BIRDS)
    suspend fun createBird(@Body bird: BirdDTO): List<BirdDTO>

    // ============================================================
    // Bird Captures
    // ============================================================

    @GET(SupabaseConfig.TABLE_BIRD_CAPTURES)
    suspend fun getBirdCaptures(
        @Query("order") order: String = "captured_at.desc",
        @Query("select") select: String = "*"
    ): List<BirdCaptureDTO>

    @GET(SupabaseConfig.TABLE_BIRD_CAPTURES)
    suspend fun getBirdCapturesByPhotographer(
        @Query("photographer_id") photographerId: String,
        @Query("order") order: String = "captured_at.desc"
    ): List<BirdCaptureDTO>

    @POST(SupabaseConfig.TABLE_BIRD_CAPTURES)
    suspend fun createBirdCapture(@Body capture: BirdCaptureDTO): List<BirdCaptureDTO>

    @PATCH(SupabaseConfig.TABLE_BIRD_CAPTURES)
    suspend fun updateBirdCapture(
        @Query("capture_id") id: String,
        @Body updates: Map<String, Any?>
    )

    // ============================================================
    // Capture Likes
    // ============================================================

    @GET(SupabaseConfig.TABLE_CAPTURE_LIKES)
    suspend fun getLikesForCapture(
        @Query("capture_id") captureId: String,
        @Query("select") select: String = "*"
    ): List<CaptureLikeDTO>

    @POST(SupabaseConfig.TABLE_CAPTURE_LIKES)
    suspend fun createLike(@Body like: CaptureLikeDTO): List<CaptureLikeDTO>

    @DELETE(SupabaseConfig.TABLE_CAPTURE_LIKES)
    suspend fun removeLike(
        @Query("capture_id") captureId: String,
        @Query("photographer_id") photographerId: String
    )

    // ============================================================
    // Capture Comments
    // ============================================================

    @GET(SupabaseConfig.TABLE_CAPTURE_COMMENTS)
    suspend fun getCommentsForCapture(
        @Query("capture_id") captureId: String,
        @Query("order") order: String = "created_at.desc"
    ): List<CaptureCommentDTO>

    @POST(SupabaseConfig.TABLE_CAPTURE_COMMENTS)
    suspend fun createComment(@Body comment: CaptureCommentDTO): List<CaptureCommentDTO>
}
