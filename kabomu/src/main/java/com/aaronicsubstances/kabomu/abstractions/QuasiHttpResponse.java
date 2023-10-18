package com.aaronicsubstances.kabomu.abstractions;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface QuasiHttpResponse extends AutoCloseable, CustomDisposable {
    
    int getStatusCode();
    void setStatusCode(int statusCode);
    Map<String, List<String>> getHeaders();
    void setHeaders(Map<String, List<String>> headers);
    String getHttpStatusMessage();
    void setHttpStatusMessage(String httpStatusMessage);
    String getHttpVersion();
    void setHttpVersion(String httpVersion);
    long getContentLength();
    void setContentLength(long contentLength);
    InputStream getBody();
    void setBody(InputStream body);
    Map<String, Object> getEnvironment();
    void setEnvironment(Map<String, Object> environment);
}
