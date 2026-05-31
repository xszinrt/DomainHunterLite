package com.example.domainhunterlite.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RdapResult(
    val domain: String,
    val exists: Boolean,
    val statusCode: Int,
    val html: String,
    val contentType: String
)

object RdapFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun check(domain: String): RdapResult {
        val url = "https://rdap.verisign.com/net/v1/domain/$domain"
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            RdapResult(
                domain = domain,
                exists = response.code == 200,
                statusCode = response.code,
                html = body,
                contentType = response.header("Content-Type", "")
            )
        } catch (e: Exception) {
            RdapResult(domain, false, 0, "", "")
        }
    }
}
