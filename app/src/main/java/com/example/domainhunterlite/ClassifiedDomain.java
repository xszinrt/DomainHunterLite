package com.example.domainhunterlite;

import java.io.Serializable;

public class ClassifiedDomain implements Serializable {
    public final String domain;
    public final DomainType type;
    public final int statusCode;
    
    public ClassifiedDomain(String domain, DomainType type, int statusCode) {
        this.domain = domain;
        this.type = type;
        this.statusCode = statusCode;
    }
}
