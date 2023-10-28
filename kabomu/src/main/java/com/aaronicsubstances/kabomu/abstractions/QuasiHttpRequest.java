package com.aaronicsubstances.kabomu.abstractions;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Represents the equivalent of an HTTP request entity: request line,
 * request headers, and request body.
 */
public interface QuasiHttpRequest extends AutoCloseable, CustomDisposable {
    
    /**
     * Gets the equivalent of request target component of HTTP request line.
     */
    String getTarget();

    /**
     * Sets the equivalent of request target component of HTTP request line.
     * @param target
     */
    void setTarget(String target);

    /**
     * Gets the equivalent of HTTP request headers.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Sets the equivalent of HTTP request headers.
     * 
     * Unlike in HTTP, headers are case-sensitive and lower-cased
     * header names are recommended.
     * 
     * Also setting a Content-Length header
     * here will have no bearing on how to transmit or receive the request body.
     */
    void setHeaders(Map<String, List<String>> headers);

    /**
     * Gets an HTTP method value.
     */
    String getHttpMethod();

    /**
     * Sets an HTTP method value.
     * @param httpMethod
     */
    void setHttpMethod(String httpMethod);

    /**
     * Gets an HTTP request version value.
     */
    String getHttpVersion();

    /**
     * Sets an HTTP request version value.
     * @param httpVersion
     */
    void setHttpVersion(String httpVersion);

    /**
     * Gets the number of bytes that the instance will supply,
     * or -1 (actually any negative value) to indicate an unknown number of
     * bytes.
     */
    long getContentLength();

    /**
     * Sets the number of bytes that the instance will supply,
     * or -1 (actually any negative value) to indicate an unknown number of
     * bytes.
     * @param contentLength
     */
    void setContentLength(long contentLength);

    /**
     * Gets the request body.
     */
    InputStream getBody();

    /**
     * Sets the request body.
     * @param body
     */
    void setBody(InputStream body);

    /**
     * Gets any objects which may be of interest during request processing.
     */
    Map<String, Object> getEnvironment();

    /**
     * Sets any objects which may be of interest during request processing.
     * @param environment
     */
    void setEnvironment(Map<String, Object> environment);
}
