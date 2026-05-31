package com.example.domainhunterlite;

import com.example.domainhunterlite.utils.RdapResult;

public class DomainClassifier {
    
    public static ClassifiedDomain classify(RdapResult result) {
        DomainType type;
        
        if (isParkedDomain(result)) {
            type = DomainType.PARKED;
        } else if (isEmptyDomain(result)) {
            type = DomainType.EMPTY;
        } else {
            type = DomainType.ACTIVE;
        }
        
        return new ClassifiedDomain(result.domain, type, result.statusCode);
    }
    
    private static boolean isParkedDomain(RdapResult result) {
        String html = result.html.toLowerCase();
        String[] keywords = {
            "domain for sale", "buy this domain", "domain parking",
            "this domain is for sale", "inquiry", "make offer",
            "sedo", "godaddy", "afternic", "dan.com"
        };
        
        for (String keyword : keywords) {
            if (html.contains(keyword)) return true;
        }
        return false;
    }
    
    private static boolean isEmptyDomain(RdapResult result) {
        String html = result.html.toLowerCase();
        
        if (result.statusCode == 404) return true;
        if (html.length() < 5000) return true;
        
        String bodyText = extractBodyText(html);
        return bodyText.length() < 100;
    }
    
    private static String extractBodyText(String html) {
        String text = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        text = text.replaceAll("(?i)<style[^>]*>.*?</style>", "");
        text = text.replaceAll("<[^>]+>", "");
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }
}
