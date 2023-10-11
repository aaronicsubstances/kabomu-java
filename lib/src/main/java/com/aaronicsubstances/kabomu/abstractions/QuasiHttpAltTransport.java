package com.aaronicsubstances.kabomu.abstractions;

public interface QuasiHttpAltTransport {
    SerializerFunction<QuasiHttpRequest> getRequestSerializer()
        throws Exception;
    SerializerFunction<QuasiHttpResponse> getResponseSerializer()
        throws Exception;
    DeserializerFunction<QuasiHttpRequest> getRequestDeserializer()
        throws Exception;
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
