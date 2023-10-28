package com.aaronicsubstances.kabomu.abstractions;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Represents the equivalent of an HTTP response entity: response status line,
 * response headers, and response body.
 */
public interface QuasiHttpResponse extends AutoCloseable, CustomDisposable {
    
    /**
     * Gets the equivalent of HTTP response status code.
     */
    int getStatusCode();

    /**
     * Sets the equivalent of HTTP response status code.
     * @param statusCode
     */
    void setStatusCode(int statusCode);

    /**
     * Gets the equivalent of HTTP response headers.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Sets the equivalent of HTTP response headers.
     * 
     * Unlike in HTTP, headers are case-sensitive and lower-cased
     * header names are recommended
     *
     * Also setting a Content-Length header
     * here will have no bearing on how to transmit or receive the response body.
     * @param headers
     */
    void setHeaders(Map<String, List<String>> headers);

    /**
     * Gets an HTTP response status text or reason phrase.
     */
    String getHttpStatusMessage();

    /**
     * Sets an HTTP response status text or reason phrase.
     * @param httpStatusMessage
     */
    void setHttpStatusMessage(String httpStatusMessage);

    /**
     * Gets an HTTP response version value.
     */
    String getHttpVersion();

    /**
     * Sets an HTTP response version value.
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
     * Gets the response body.
     */
    InputStream getBody();

    /**
     * Sets the response body.
     * @param body
     */
    void setBody(InputStream body);

    /**
     * Gets any objects which may be of interest during response processing.
     */
    Map<String, Object> getEnvironment();

    /**
     * Sets any objects which may be of interest during response processing.
     * @param environment
     */
    void setEnvironment(Map<String, Object> environment);
}
