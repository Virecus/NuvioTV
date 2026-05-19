package com.nuvio.tv.data.repository

import com.nuvio.tv.domain.model.TraktCommentReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraktCommentTranslationService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun translateCommentsToTurkish(
        comments: List<TraktCommentReview>
    ): List<TraktCommentReview> = coroutineScope {
        comments.chunked(8).flatMap { chunk ->
            chunk.map { review ->
                async(Dispatchers.IO) {
                    val translated = translateToTurkish(review.comment)
                    if (translated.isBlank() || translated == review.comment) {
                        review
                    } else {
                        review.copy(comment = translated)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun translateToTurkish(text: String): String {
        if (text.isBlank()) return text
        cache[text]?.let { return it }

        val translated = withContext(Dispatchers.IO) {
            val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.name())
            val request = Request.Builder()
                .url(
                    "https://translate.googleapis.com/translate_a/single" +
                        "?client=gtx&sl=auto&tl=tr&dt=t&q=$encodedText"
                )
                .get()
                .build()

            runCatching {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use text
                    val body = response.body?.string().orEmpty()
                    parseTranslatedText(body).ifBlank { text }
                }
            }.getOrDefault(text)
        }

        cache[text] = translated
        return translated
    }

    private fun parseTranslatedText(body: String): String {
        if (body.isBlank()) return ""
        val root = JSONArray(body)
        val translatedSegments = root.optJSONArray(0) ?: return ""
        return buildString {
            for (index in 0 until translatedSegments.length()) {
                val segment = translatedSegments.optJSONArray(index) ?: continue
                append(segment.optString(0))
            }
        }.trim()
    }
}
