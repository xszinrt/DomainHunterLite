package com.example.domainhunterlite.utils;

public class RdapResult {
    public final String domain;
    public final boolean exists;
    public final int statusCode;
    public final String html;
    public final String contentType;
    
    public RdapResult(String domain, boolean exists, int statusCode, String html, String contentType) {
        this.domain = domain;
        this.exists = exists;
        this.statusCode = statusCode;
        this.html = html;
        this.contentType = contentType;
    }
}
