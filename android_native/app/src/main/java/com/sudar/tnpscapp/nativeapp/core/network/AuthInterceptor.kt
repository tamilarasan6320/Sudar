package com.sudar.tnpscapp.nativeapp.core.network

import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val headers = runBlocking { sessionStore.authHeaders() }

        val request = original.newBuilder().apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()

        return chain.proceed(request)
    }
}

