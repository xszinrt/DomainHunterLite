package com.example.domainhunterlite.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class RdapFetcher {
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build();
    
    public static RdapResult check(String domain) {
        String url = "https://rdap.verisign.com/net/v1/domain/" + domain;
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                
                return new RdapResult(
                    domain,
                    response.code() == 200,
                    response.code(),
                    body,
                    response.header("Content-Type", "")
                );
            }
        } catch (Exception e) {
            return new RdapResult(domain, false, 0, "", "");
        }
    }
}
