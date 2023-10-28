package com.aaronicsubstances.kabomu.abstractions;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Provides default implementation of {@link QuasiHttpRequest}
 * interface.
 */
public class DefaultQuasiHttpRequest implements QuasiHttpRequest {
    private String target;
    private Map<String, List<String>> headers;
    private String httpMethod;
    private String httpVersion;
    private long contentLength;
    private InputStream body;
    private Map<String, Object> environment;
    private CheckedRunnable disposer;
    public String getTarget() {
        return target;
    }
    public void setTarget(String target) {
        this.target = target;
    }
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }
    public String getHttpMethod() {
        return httpMethod;
    }
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    public String getHttpVersion() {
        return httpVersion;
    }
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }
    public long getContentLength() {
        return contentLength;
    }
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
    public InputStream getBody() {
        return body;
    }
    public void setBody(InputStream body) {
        this.body = body;
    }
    public Map<String, Object> getEnvironment() {
        return environment;
    }
    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }
    public CheckedRunnable getDisposer() {
        return disposer;
    }
    public void setDisposer(CheckedRunnable disposer) {
        this.disposer = disposer;
    }
    @Override
    public void close() throws Exception {
        if (disposer != null) {
            disposer.run();
        }
    }
}
