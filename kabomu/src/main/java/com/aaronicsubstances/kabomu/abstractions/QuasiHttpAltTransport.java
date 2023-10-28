package com.aaronicsubstances.kabomu.abstractions;

import com.aaronicsubstances.kabomu.StandardQuasiHttpClient;
import com.aaronicsubstances.kabomu.StandardQuasiHttpServer;

/**
 * Represents additional interface that transport property of 
 * {@link StandardQuasiHttpClient} and
 * {@link StandardQuasiHttpServer} classes can
 * implement, in order to override parts of
 * {@link QuasiHttpTransport} functionality.
 */
public interface QuasiHttpAltTransport {

    /**
     * Gets a function which can return true to
     * prevent the need to write request headers
     * and body to a connection.
     * @throws Exception
     */
    SerializerFunction<QuasiHttpRequest> getRequestSerializer()
        throws Exception;

    /**
     * Gets a function which can return true to prevent the
     * need to write response headers and body to a connection.
     * @throws Exception
     */
    SerializerFunction<QuasiHttpResponse> getResponseSerializer()
        throws Exception;

    /**
     * Gets a function which can return a non-null request object to
     * prevent the need to read request headers from a connection.
     * @throws Exception
     */
    DeserializerFunction<QuasiHttpRequest> getRequestDeserializer()
        throws Exception;

    /**
     * Gets a function which can return a non-null response object
     * to prevent the need to read response headers from  a
     * connection.
     * @throws Exception
     */
    DeserializerFunction<QuasiHttpResponse> getResponseDeserializer()
        throws Exception;

    @FunctionalInterface
    public static interface SerializerFunction<T> {
        Boolean apply(QuasiHttpConnection connection, T entity) throws Exception;
    }
    
    @FunctionalInterface
    public static interface DeserializerFunction<T> {
        T apply(QuasiHttpConnection connection) throws Exception;
    }
}
