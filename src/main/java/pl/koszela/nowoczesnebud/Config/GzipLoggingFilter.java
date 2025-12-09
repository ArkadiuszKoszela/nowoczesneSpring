package pl.koszela.nowoczesnebud.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * FILTER DO LOGOWANIA GZIP
 * 
 * Sprawdza czy response jest kompresowany GZIP i loguje rozmiar przed/po kompresji
 */
@Component
@Order(1)
public class GzipLoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(GzipLoggingFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Sprawdź czy klient akceptuje GZIP
        String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
        boolean clientAcceptsGzip = acceptEncoding != null && acceptEncoding.contains("gzip");
        
        // Wrapper do przechwytywania Content-Encoding
        ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);
        
        // Wykonaj request
        chain.doFilter(request, responseWrapper);
        
        // Sprawdź czy response jest GZIP
        String contentEncoding = responseWrapper.getHeader("Content-Encoding");
        boolean isGzipped = contentEncoding != null && contentEncoding.contains("gzip");
        
        // Loguj tylko dla API endpoints z dużymi danymi
        String uri = httpRequest.getRequestURI();
        if (uri.contains("/products-comparison") || uri.contains("/fill-quantities")) {
            logger.info("🗜️ GZIP Check [{}]:", uri);
            logger.info("   Client Accept-Encoding: {}", acceptEncoding);
            logger.info("   Response Content-Encoding: {}", contentEncoding);
            logger.info("   GZIP Active: {}", isGzipped ? "✅ YES" : "❌ NO");
            
            if (!clientAcceptsGzip) {
                logger.warn("   ⚠️ Client nie akceptuje GZIP! Sprawdź Angular HttpClient headers");
            }
            if (!isGzipped && clientAcceptsGzip) {
                logger.warn("   ⚠️ GZIP nie działa mimo że klient akceptuje! Sprawdź TomcatGzipConfig");
            }
        }
    }
    
    /**
     * Wrapper do przechwytywania response headers
     */
    private static class ResponseWrapper extends HttpServletResponseWrapper {
        public ResponseWrapper(HttpServletResponse response) {
            super(response);
        }
    }
}


