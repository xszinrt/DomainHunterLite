package com.example.domainhunterlite

import com.example.domainhunterlite.utils.RdapResult

enum class DomainType {
    EMPTY,      // موقع فارغ
    PARKED,     // للإيجار
    ACTIVE      // يحتوي محتوى
}

data class ClassifiedDomain(
    val domain: String,
    val type: DomainType,
    val statusCode: Int
)

object DomainClassifier {
    
    fun classify(result: RdapResult): ClassifiedDomain {
        val type = when {
            isParkedDomain(result) -> DomainType.PARKED
            isEmptyDomain(result) -> DomainType.EMPTY
            else -> DomainType.ACTIVE
        }
        
        return ClassifiedDomain(
            domain = result.domain,
            type = type,
            statusCode = result.statusCode
        )
    }
    
    private fun isParkedDomain(result: RdapResult): Boolean {
        val html = result.html.lowercase()
        val parkedKeywords = listOf(
            "domain for sale", "buy this domain", "domain parking",
            "this domain is for sale", "inquiry", "make offer",
            "sedo", "godaddy", "afternic", "dan.com"
        )
        
        return parkedKeywords.any { html.contains(it) }
    }
    
    private fun isEmptyDomain(result: RdapResult): Boolean {
        val html = result.html.lowercase()
        
        // صفحة فارغة أو خطأ 404
        if (result.statusCode == 404) return true
        
        // حجم الصفحة صغير جداً (أقل من 5KB)
        if (html.length < 5000) return true
        
        // محتوى فارغ أو قليل جداً
        val bodyText = extractBodyText(html)
        if (bodyText.length < 100) return true
        
        return false
    }
    
    private fun extractBodyText(html: String): String {
        // إزالة script و style
        var text = html.replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<style[^>]*>.*?</style>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<[^>]+>"), "")
        text = text.replace(Regex("\\s+"), " ")
        return text.trim()
    }
}
