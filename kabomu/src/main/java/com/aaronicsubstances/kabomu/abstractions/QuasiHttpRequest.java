package com.aaronicsubstances.kabomu.abstractions;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface QuasiHttpRequest extends AutoCloseable, CustomDisposable {
    
    String getTarget();
    void setTarget(String target);
    Map<String, List<String>> getHeaders();
    void setHeaders(Map<String, List<String>> headers);
    String getHttpMethod();
    void setHttpMethod(String httpMethod);
    String getHttpVersion();
    void setHttpVersion(String httpVersion);
    long getContentLength();
    void setContentLength(long contentLength);
    InputStream getBody();
    void setBody(InputStream body);
    Map<String, Object> getEnvironment();
    void setEnvironment(Map<String, Object> environment);
}
