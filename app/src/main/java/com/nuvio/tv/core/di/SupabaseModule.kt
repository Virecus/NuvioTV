package com.nuvio.tv.core.di

import com.nuvio.tv.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    @OptIn(SupabaseInternal::class)
    fun provideSupabaseClient(): SupabaseClient {
        val userAgent = "NuvioTV/${BuildConfig.VERSION_NAME.ifBlank { "dev" }}"
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
                defaultRequest {
                    headers.append(HttpHeaders.UserAgent, userAgent)
                }
                if (BuildConfig.DEBUG) {
                    val trustAll = object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                    val sslContext = SSLContext.getInstance("TLS").apply {
                        init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
                    }
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
                }
            }
            install(Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                autoSaveToStorage = true
                enableLifecycleCallbacks = false
            }
            install(Postgrest)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth = client.auth

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient): Postgrest = client.postgrest
}
